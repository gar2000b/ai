/** Matrix Exponentiation Fibonacci - Version 8 */
public class FibonacciMatrix {
    public static void main(String[] args) {
        int n = 10;
        System.out.print(fibonacci(n) + " ");
    }

    static int fibonacci(int n) {
        if (n == 0) return 0;
        int[][] result = {{1, 0}, {0, 1}}; // Identity matrix
        int[][] fibMatrix = {{1, 1}, {1, 0}};
        while (n > 0) {
            if (n % 2 == 1) {
                result = multiply(result, fibMatrix);
            }
            fibMatrix = multiply(fibMatrix, fibMatrix);
            n /= 2;
        }
        return result[0][1];
    }

    static int[][] multiply(int[][] a, int[][] b) {
        int[][] c = new int[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                c[i][j] = 0;
                for (int k = 0; k < 2; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return c;
    }
}
