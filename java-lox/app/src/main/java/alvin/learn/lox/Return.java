package alvin.learn.lox;

class Return extends RuntimeException {
    public final Object value;
    public Return(Object value) {
        this.value = value;
    }
}