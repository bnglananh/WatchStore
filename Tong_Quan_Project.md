# BÁO CÁO TỔNG QUAN PROJECT: WEBSITE BÁN ĐỒNG HỒ (WATCH STORE)
Đây là tài liệu phân tích kỹ thuật chi tiết dành cho bạn để dễ dàng nắm bắt toàn bộ luồng xử lý (flow), các công nghệ cốt lõi và kiến trúc Database của dự án phục vụ cho việc báo cáo đồ án.

---

## 1. CÔNG NGHỆ VÀ CƠ CHẾ SỬ DỤNG TRONG PROJECT

Dự án được xây dựng dựa trên hệ sinh thái **Java Spring Boot** với mô hình MVC (Model - View - Controller). Dưới đây là các công cụ và cơ chế cốt lõi được sử dụng:

1. **Spring Boot (Core Framework):** Tự động cấu hình và quản lý các bean, giúp triển khai ứng dụng nhanh chóng.
2. **Spring MVC:** Xử lý luồng dữ liệu giữa giao diện (View) và các Controller.
3. **Thymeleaf:** View Template Engine. Nhận dữ liệu từ Controller đưa vào HTML thông qua các thẻ attribute của Thymeleaf (`th:text`, `th:each`, `th:if`, v.v.).
4. **Spring Security:** Quản lý đăng nhập, phân quyền và bảo vệ các đường dẫn (URL). 
   - *Cơ chế phân quyền:* Sử dụng dựa trên Role (như `ROLE_USER`, `ROLE_ADMIN`).
   - *Mã hóa mật khẩu:* Dùng `BCryptPasswordEncoder` để băm mật khẩu thành chuỗi hash ngẫu nhiên không thể dịch ngược, đảm bảo bảo mật.
   - *Tính năng:* Có hỗ trợ chức năng ghi nhớ phiên đăng nhập (Remember Me) và phân tách luồng truy cập giữa Admin và User bình thường.
5. **Spring Data JPA:** Giúp tương tác với cơ sở dữ liệu thông qua các interface Repository (như `UserRepository`, `ProductRepository`) mà không cần phải viết SQL thủ công.
6. **Validation:** Sử dụng các Annotation (`@NotBlank`, `@Email`, `@Size`, `@Min`, `@Valid`) để kiểm tra dữ liệu đầu vào trực tiếp tại Controller trước khi lưu vào Database, tránh dữ liệu rác.
7. **OAuth2:** Hỗ trợ đăng nhập qua nền tảng mạng xã hội (Google, Facebook).

---

## 2. QUAN HỆ CÁC BẢNG TRONG DATABASE

Database được thiết kế chuẩn hóa cho một hệ thống E-Commerce thương mại điện tử. Dưới đây là mối quan hệ cụ thể giữa các bảng chính:

- **Bảng `user` và `role`:** Quan hệ **nhiều-nhiều (N-N)** thông qua bảng trung gian `user_role`. Một user có thể có nhiều quyền, một quyền có thể được gán cho nhiều user. (Dùng cho Spring Security cấp quyền truy cập).
- **Bảng `categories` và `products`:** Quan hệ **một-nhiều (1-N)**. Một danh mục chứa nhiều sản phẩm (Foreign key: `category_id` trong bảng `products`).
- **Bảng `products` và `product_images`:** Quan hệ **một-nhiều (1-N)**. Một sản phẩm có nhiều ảnh chi tiết (Foreign key: `product_id` trong bảng `product_images`).
- **Bảng `user` và `carts`:** Quan hệ **một-một (1-1)** hoặc **một-nhiều (1-N)**. Mỗi user có một giỏ hàng đang active.
- **Bảng `carts` và `cart_items`:** Quan hệ **một-nhiều (1-N)**. Một giỏ hàng có nhiều mặt hàng. `cart_items` lưu `cart_id` và `product_id` cùng với số lượng (`quantity`).
- **Bảng `user` và `orders`:** Quan hệ **một-nhiều (1-N)**. Một user có thể đặt nhiều đơn hàng.
- **Bảng `orders` và `order_details`:** Quan hệ **một-nhiều (1-N)**. Đơn hàng sẽ có nhiều chi tiết mặt hàng khác nhau. `order_details` lưu trữ giá trị tại thời điểm mua, số lượng và `product_id`.
- **Bảng `user`, `products` và `reviews`:** Quan hệ **một-nhiều (1-N)**. Một user đánh giá nhiều sản phẩm, một sản phẩm có nhiều đánh giá. Bảng `reviews` làm trung gian chứa `user_id` và `product_id`.
- **Bảng `coupons`:** Quản lý mã giảm giá, áp dụng ở cấp độ Order.

---

## 3. CHI TIẾT CÁC FLOW (LUỒNG XỬ LÝ) CHỨC NĂNG

Flow chung của mô hình: **Client (Giao diện) -> Controller (Điều hướng) -> Service (Nghiệp vụ) -> Repository (Database) -> Model -> Controller -> Client (View).**

### 3.1. Luồng Xác thực (Authentication Flow)
* **Đăng ký:** `User` điền form đăng ký (`/register`) -> `UserController` hứng request -> Xác thực thông tin bằng Validation -> Mã hóa Password bằng `BCryptPasswordEncoder` -> Gọi `UserService` lưu thông tin vào bảng `user` -> Phân quyền mặc định là `ROLE_USER` -> Lưu DB.
* **Đăng nhập:** Truy cập `/login` -> Spring Security chặn lại và xác thực -> Dựa trên cấu hình `SecurityConfig`, đối chiếu thông tin trong `user` table (mật khẩu đã BCrypt) -> Cấp `Session` (Phiên làm việc) và Cookie `Remember-Me` nếu user tick chọn -> Chuyển hướng theo Role (Admin vào Dashboard, User vào Trang chủ).
* **Quên mật khẩu:** `ForgotPasswordController` -> Người dùng nhập email -> `EmailService` tạo và gửi mã OTP qua Mail -> User nhập OTP xác thực thành công -> Form đổi mật khẩu -> Cập nhật hash password mới vào DB.

### 3.2. Luồng Khách hàng (User/Customer Flow)
* **Trang chủ & Danh mục (`HomeController`, `UserProductController`):** Truy cập web -> Lấy dữ liệu top sản phẩm, danh mục từ `ProductService`, `CategoryService` -> Trả về model cho file Thymeleaf hiển thị.
* **Thêm vào Giỏ hàng (`CartController`):** Xem chi tiết sản phẩm -> Bấm "Thêm vào giỏ" -> Controller bắt action kèm `product_id` và `quantity` -> Kiểm tra `Stock` (số lượng tồn kho) -> Nếu hợp lệ, lưu vào `cart_items` của User hiện tại -> Cập nhật tổng tiền giỏ hàng.
* **Thanh toán & Đặt hàng (`OrderController`):** Bấm Thanh toán -> Điền thông tin giao hàng & Mã giảm giá (`Coupon`) -> Bấm Đặt hàng -> Chuyển dữ liệu từ `cart_items` sang bảng `order_details` -> Tạo mới 1 dòng trong `orders` với trạng thái **Pending (Chờ xác nhận)** -> Trừ Stock ở bảng `products` -> Xóa sản phẩm khỏi giỏ hàng.
* **Đánh giá (`ReviewController`):** User vào trang đơn hàng đã "Completed" -> Nhập số sao & Bình luận -> Gửi về Controller -> Lưu vào bảng `reviews` (chờ Admin duyệt).

### 3.3. Luồng Quản trị viên (Admin Flow)
*Tất cả luồng Admin đều yêu cầu tài khoản phải có quyền `ROLE_ADMIN` để pass qua lớp bảo vệ của Spring Security.*

* **Dashboard (`AdminDashboardController` - `/admin/dashboard`):** Khởi tạo khi Admin đăng nhập thành công. Gọi nhiều Services để lấy thống kê: Doanh thu, số đơn chờ xử lý, số sản phẩm tồn kho < 5, Top sản phẩm bán chạy -> Format dữ liệu truyền xuống biểu đồ bằng Chart.js.
* **Quản lý Sản phẩm (`ProductController` - `/admin/products`):**
    - **Add/Edit:** Admin vào trang Thêm -> Controller lấy danh sách `categories` để hiển thị ở Dropdown -> Khi submit form lưu: Tự động chạy Validation (`@Valid`) -> Upload mảng `MultipartFile[]` (hình ảnh) vào file server, lưu Path (đường dẫn) vào DB `product_images` -> Lưu dữ liệu mô tả vào `products`.
    - **Delete:** Gọi `ProductService` -> Xóa mềm hoặc xóa cứng khỏi CSDL.
* **Quản lý Đơn hàng (`AdminOrderController` - `/admin/orders`):**
    - **Xem/Lọc:** Hiển thị danh sách tất cả đơn hàng, có bộ lọc theo email khách hàng.
    - **Đổi trạng thái:** Flow: Chờ xác nhận -> Đã xác nhận -> Đang giao -> Hoàn thành. Admin thao tác tại `/update-status` -> Cập nhật status trong DB.
* **Quản lý Người dùng (`AdminUserController` - `/admin/users`):** Hiển thị danh sách khách hàng. Admin có quyền thực thi `/toggle/{id}` để Khóa/Mở Khóa một tài khoản vi phạm.
* **Quản lý Đánh giá (`AdminReviewController` - `/admin/reviews`):** Danh sách các Review của khách chờ duyệt. Admin bấm Duyệt (`/approve/{id}`) để review được phép hiển thị trên trang chi tiết sản phẩm, hoặc bấm Xóa nếu ngôn từ vi phạm.
* **Quản lý Mã giảm giá (`AdminCouponController`):** CRUD (Thêm, sửa, xóa, lấy danh sách) mã giảm giá với số lượng giảm, ngày hết hạn.

---
## Tóm tắt để thuyết trình (Mẹo báo cáo)
1. Hãy nhấn mạnh vào việc bạn sử dụng **Spring Security** với **BCrypt** làm cơ chế bảo mật (rất điểm).
2. Khi báo cáo luồng đặt hàng, nhớ nhắc đến thao tác: **"Tạo đơn hàng -> Trừ số lượng tồn kho (Stock) -> Xóa giỏ hàng"**, điều này cho thấy bạn xử lý logic nghiệp vụ chặt chẽ.
3. Nếu giảng viên hỏi tại sao hình ảnh không lưu trong DB: Trả lời rằng bạn **chỉ lưu đường dẫn ảnh** trong DB và lưu file vật lý trên server để tối ưu hiệu suất, tránh phình to Database.
