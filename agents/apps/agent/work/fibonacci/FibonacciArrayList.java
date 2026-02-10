/** Iterative with ArrayList - Version 10 */
import java.util.ArrayList;

public class FibonacciArrayList {
    public static void main(String[] args) {
        int count = 11;
        ArrayList<Integer> fibSeq = new ArrayList<>();
        fibSeq.add(0);
        fibSeq.add(1);
        for (int i = 2; i < count; i++) {
            fibSeq.add(fibSeq.get(i - 1) + fibSeq.get(i - 2));
        }
        for (int num : fibSeq) {
            System.out.print(num + " ");
        }
    }
}
