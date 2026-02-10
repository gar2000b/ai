/** Memoization Fibonacci - Version 5 */
import java.util.HashMap;
import java.util.Map;

public class FibonacciMemo {
    private static Map<Integer, Integer> memo = new HashMap<>();

    public static void main(String[] args) {
        int count = 20;
        for (int i = 0; i < count; i++) {
            System.out.print(fibonacci(i) + " ");
        }
    }

    public static int fibonacci(int n) {
        if (memo.containsKey(n)) {
            return memo.get(n);
        }
        if (n <= 1) {
            memo.put(n, n);
            return n;
        }
        int result = fibonacci(n - 1) + fibonacci(n - 2);
        memo.put(n, result);
        return result;
    }
}
