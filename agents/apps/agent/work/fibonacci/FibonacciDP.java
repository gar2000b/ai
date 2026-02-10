/** Dynamic Programming Fibonacci - Version 3 */
public class FibonacciDP {
    public static void main(String[] args) {
        int count = 12;
        int[] fib = new int[count];
        fib[0] = 0;
        fib[1] = 1;
        for (int i = 2; i < count; i++) {
            fib[i] = fib[i - 1] + fib[i - 2];
        }
        for (int i = 0; i < count; i++) {
            System.out.print(fib[i] + " ");
        }
    }
}
