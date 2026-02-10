/** Recursive Fibonacci with Class Encapsulation - Version 9 */
public class FibonacciEncapsulated {
    private int n;

    public FibonacciEncapsulated(int n) {
        this.n = n;
    }

    public int compute() {
        if (n <= 1) {
            return n;
        }
        return new FibonacciEncapsulated(n - 1).compute() + new FibonacciEncapsulated(n - 2).compute();
    }

    public static void main(String[] args) {
        int total = 14;
        for (int i = 0; i < total; i++) {
            System.out.print(new FibonacciEncapsulated(i).compute() + " ");
        }
    }
}
