import org.jetbrains.annotations.NotNull;

class WithNotNulls {
    @NotNull
    public String foo(@NotNull String s) {
        return s + "x";
    }

    public static void main(String[] args) {
        WithNotNulls instance = new WithNotNulls();
        System.out.println(instance.foo("z"));
    }
}
