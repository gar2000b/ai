/**
 * Test.java - Sample file for testing update_code_section tool.
 * Contains trivial code with 4 agentic workflow edit sections.
 */
public class Test {

    private String name;
    private int count;
    private double value;

    public Test() {
        this.name = "default";
        this.count = 0;
        this.value = 0.0;
    }

    public Test(String name, int count, double value) {
        this.name = name;
        this.count = count;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    // Begin Agentic Workflow Edit - 1.
    // Placeholder for section 1 - replace this with your logic
    public void sectionOne() {
        System.out.println("Section 1: " + name);
    }
    // End Agentic Workflow Edit - 1.

    public void doSomething() {
        count++;
        value += 0.5;
    }

    public void doSomethingElse(String arg) {
        name = arg;
        count = 0;
    }

    public int add(int a, int b) {
        return a + b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    // Begin Agentic Workflow Edit - 2.
    // Placeholder for section 2 - replace this with your logic
    public String sectionTwo() {
        return "Section 2: " + count;
    }
    // End Agentic Workflow Edit - 2.

    public boolean isEmpty() {
        return name == null || name.isEmpty();
    }

    public void reset() {
        name = "default";
        count = 0;
        value = 0.0;
    }

    public String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public int[] makeArray(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }
        return arr;
    }

    // Begin Agentic Workflow Edit - 3.
    // Placeholder for section 3 - replace this with your logic
    public void sectionThree(int x) {
        System.out.println("Section 3: " + x);
    }
    // End Agentic Workflow Edit - 3.

    public void process() {
        for (int i = 0; i < count; i++) {
            value += 1.0;
        }
    }

    public String format() {
        return String.format("Test[name=%s, count=%d, value=%.2f]", name, count, value);
    }

    public Test copy() {
        return new Test(name, count, value);
    }

    public void merge(Test other) {
        this.count += other.count;
        this.value += other.value;
    }

    public static int staticAdd(int a, int b) {
        return a + b;
    }

    public static String staticGreet(String who) {
        return "Hello, " + who;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public double divide(double a, double b) {
        return b != 0 ? a / b : 0;
    }

    public boolean isPositive(int n) {
        return n > 0;
    }

    public boolean isNegative(int n) {
        return n < 0;
    }

    public int max(int a, int b) {
        return a > b ? a : b;
    }

    public int min(int a, int b) {
        return a < b ? a : b;
    }

    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public double square(double x) {
        return x * x;
    }

    public int sum(int[] arr) {
        int s = 0;
        for (int n : arr) {
            s += n;
        }
        return s;
    }

    public int findMax(int[] arr) {
        if (arr.length == 0) return 0;
        int m = arr[0];
        for (int n : arr) {
            if (n > m) m = n;
        }
        return m;
    }

    public void swap(String[] arr, int i, int j) {
        String tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    public String concatenate(String a, String b) {
        return a + b;
    }

    public int length(String s) {
        return s != null ? s.length() : 0;
    }

    public boolean equals(Test other) {
        return other != null && name.equals(other.name) && count == other.count && value == other.value;
    }

    public void scale(double factor) {
        value *= factor;
    }

    public void normalize() {
        if (count > 0) {
            value /= count;
        }
    }

    public String toUpper(String s) {
        return s != null ? s.toUpperCase() : null;
    }

    public String toLower(String s) {
        return s != null ? s.toLowerCase() : null;
    }

    public boolean contains(String s, String sub) {
        return s != null && sub != null && s.contains(sub);
    }

    public String trim(String s) {
        return s != null ? s.trim() : null;
    }

    public int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void clear() {
        name = "";
        count = 0;
        value = 0.0;
    }

    public boolean hasValue() {
        return value != 0.0;
    }

    public boolean hasCount() {
        return count != 0;
    }

    // Begin Agentic Workflow Edit - 4.
    public void sectionFour() {
        System.out.println("This REALLY is Section 4: " + format());
    }
    // End Agentic Workflow Edit - 4.

    public void run() {
        sectionOne();
        System.out.println(sectionTwo());
        sectionThree(42);
        sectionFour();
    }

    @Override
    public String toString() {
        return format();
    }

    public static void main(String[] args) {
        Test t = new Test("test", 5, 10.5);
        t.run();
    }
}
