public class AssertionFailedError extends AssertionError {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new AssertionFailedError without a detail message.
   */
  public AssertionFailedError() {
  }

  /**
   * Constructs a new AssertionFailedError with the specified detail message.
   * A null message is replaced by an empty String.
   * @param message the detail message. The detail message is saved for later
   * retrieval by the {@code Throwable.getMessage()} method.
   */
  public AssertionFailedError(String message) {
    super(defaultString(message));
  }

  public static String defaultString(String message) {
    return message == null ? "" : message;
  }
}