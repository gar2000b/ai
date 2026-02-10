/** Iterative Fibonacci Implementation - Version 2 */
public class FibonacciIterative {
    public static void main(String[] args) {
        int count = 15; // Total number of Fibonacci numbers to generate
        int a = 0, b = 1;
        System.out.print("" + a + " " + b);
        for (int i = 2; i < count; i++) {
            int next = a + b;
            System.out.print(" " + next);
            a = b;
            b = next;
        }
    }
}
