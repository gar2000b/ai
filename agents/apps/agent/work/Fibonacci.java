public class Fibonacci {
    public static void main(String[] args) {
        int n = 10; // Number of Fibonacci numbers to generate
        int a = 0, b = 1;
        System.out.println("Fibonacci sequence:");
        for (int i = 0; i < n; i++) {
            System.out.print(a + " ");
            int next = a + b;
            a = b;
            b = next;
        }
    }
}