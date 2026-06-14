package WEB.WATCH.service;

import WEB.WATCH.model.Cart;
import WEB.WATCH.model.CartItem;
import WEB.WATCH.model.Product;
import WEB.WATCH.model.Coupon;
import WEB.WATCH.model.User;
import WEB.WATCH.repository.CartRepository;
import WEB.WATCH.repository.CartItemRepository;
import WEB.WATCH.repository.ProductRepository;
import WEB.WATCH.repository.IUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final IUserRepository userRepository;
    private final HttpServletRequest request;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                return userRepository.findByUsername(((UserDetails) principal).getUsername()).orElse(null);
            } else if (principal instanceof OAuth2User) {
                String email = ((OAuth2User) principal).getAttribute("email");
                return userRepository.findByEmail(email).orElse(null);
            }
        }
        return null;
    }

    private String getSessionId() {
        return request.getSession().getId();
    }

    @Transactional
    public Cart getCart() {
        User user = getCurrentUser();
        Cart cart = null;
        if (user != null) {
            cart = cartRepository.findByUser(user).orElse(null);
            if (cart == null) {
                cart = new Cart();
                cart.setUser(user);
                
                // Merge session cart if exists
                Cart sessionCart = cartRepository.findBySessionId(getSessionId()).orElse(null);
                if (sessionCart != null) {
                    for (CartItem sessionItem : sessionCart.getItems()) {
                        boolean found = false;
                        for (CartItem existingItem : cart.getItems()) {
                            if (existingItem.getProduct().getId().equals(sessionItem.getProduct().getId())) {
                                int newQty = existingItem.getQuantity() + sessionItem.getQuantity();
                                if (newQty > existingItem.getProduct().getStock()) {
                                    newQty = existingItem.getProduct().getStock();
                                }
                                existingItem.setQuantity(newQty);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            sessionItem.setCart(cart);
                            cart.getItems().add(sessionItem);
                        }
                    }
                    
                    if (cart.getAppliedCoupon() == null) {
                        cart.setAppliedCoupon(sessionCart.getAppliedCoupon());
                    }
                    // Clear the session cart items to avoid constraint violation on delete
                    sessionCart.getItems().clear();
                    cartRepository.delete(sessionCart);
                }
                cart = cartRepository.save(cart);
            }
        } else {
            String sessionId = getSessionId();
            cart = cartRepository.findBySessionId(sessionId).orElse(null);
            if (cart == null) {
                cart = new Cart();
                cart.setSessionId(sessionId);
                cart = cartRepository.save(cart);
            }
        }
        return cart;
    }

    @Transactional
    public void addToCart(Product product, int quantity) {
        Cart cart = getCart();
        Product latestProduct = productRepository.findById(product.getId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));
        
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(latestProduct.getId())) {
                int newQuantity = item.getQuantity() + quantity;
                if (newQuantity > latestProduct.getStock()) {
                    throw new IllegalArgumentException("Vượt quá số lượng tồn kho (" + latestProduct.getStock() + ")!");
                }
                item.setQuantity(newQuantity);
                cartItemRepository.save(item);
                return;
            }
        }
        
        if (quantity > latestProduct.getStock()) {
            throw new IllegalArgumentException("Vượt quá số lượng tồn kho (" + latestProduct.getStock() + ")!");
        }
        CartItem newItem = new CartItem(cart, latestProduct, quantity);
        cart.getItems().add(newItem);
        cartItemRepository.save(newItem);
    }

    @Transactional
    public void updateQuantity(Long productId, int quantity) {
        Cart cart = getCart();
        Product latestProduct = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        CartItem itemToRemove = null;
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId)) {
                if (quantity > latestProduct.getStock()) {
                    throw new IllegalArgumentException("Vượt quá số lượng tồn kho (" + latestProduct.getStock() + ")!");
                }
                item.setQuantity(quantity);
                if (item.getQuantity() <= 0) {
                    itemToRemove = item;
                } else {
                    cartItemRepository.save(item);
                }
                break;
            }
        }
        
        if (itemToRemove != null) {
            cart.getItems().remove(itemToRemove);
            cartItemRepository.delete(itemToRemove);
        }
        
        validateCoupon(cart);
    }

    @Transactional
    public void removeFromCart(Long productId) {
        Cart cart = getCart();
        CartItem itemToRemove = null;
        for (CartItem item : cart.getItems()) {
            if (item.getProduct().getId().equals(productId)) {
                itemToRemove = item;
                break;
            }
        }
        if (itemToRemove != null) {
            cart.getItems().remove(itemToRemove);
            cartItemRepository.delete(itemToRemove);
        }
        validateCoupon(cart);
    }

    @Transactional
    public void clearCart() {
        Cart cart = getCart();
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);
    }

    @Transactional
    public List<CartItem> getItems() {
        return getCart().getItems();
    }

    @Transactional
    public void applyCoupon(Coupon coupon) {
        Cart cart = getCart();
        cart.setAppliedCoupon(coupon);
        cartRepository.save(cart);
    }

    @Transactional
    public void removeCoupon() {
        Cart cart = getCart();
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);
    }

    @Transactional
    public Coupon getAppliedCoupon() {
        return getCart().getAppliedCoupon();
    }

    @Transactional
    public double getTotalPrice() {
        return getCart().getItems().stream()
                .mapToDouble(item -> {
                    Double price = (item.getProduct().getDiscountPrice() != null && item.getProduct().getDiscountPrice() > 0) 
                                   ? item.getProduct().getDiscountPrice() 
                                   : item.getProduct().getPrice();
                    return (price != null ? price : 0) * item.getQuantity();
                })
                .sum();
    }

    @Transactional
    public double getDiscountAmount() {
        Coupon appliedCoupon = getCart().getAppliedCoupon();
        if (appliedCoupon == null) return 0.0;
        return appliedCoupon.calculateDiscount(getTotalPrice());
    }

    @Transactional
    public double getGrandTotal() {
        return Math.max(0, getTotalPrice() - getDiscountAmount());
    }

    private void validateCoupon(Cart cart) {
        Coupon appliedCoupon = cart.getAppliedCoupon();
        if (appliedCoupon != null && !appliedCoupon.isValid(getTotalPrice())) {
            cart.setAppliedCoupon(null);
            cartRepository.save(cart);
        }
    }
}
