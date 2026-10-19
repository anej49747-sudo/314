package csit314.renovation.category.control;

public class DuplicateCategoryException extends RuntimeException {
    public DuplicateCategoryException() {
        super("A category with this name already exists");
    }
}
