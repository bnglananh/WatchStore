package WEB.WATCH.repository;
import WEB.WATCH.model.Category;
import org.springframework.data.jpa.repository.JpaRepository; import
        org.springframework.stereotype.Repository;
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> { }


