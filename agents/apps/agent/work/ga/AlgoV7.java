package com.onlineinteract.ga.base;

import com.onlineinteract.adt.TokenData;
import com.onlineinteract.utility.TokenDataFetcher;
import org.jetbrains.annotations.NotNull;
import org.uncommons.maths.binary.BitString;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.*;
import org.uncommons.watchmaker.framework.operators.BitStringCrossover;
import org.uncommons.watchmaker.framework.operators.BitStringMutation;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.termination.GenerationCount;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AlgoV7: simple 3-parameter RSI mean-reversion strategy.
 *
 * Strategy:
 *   - Compute RSI(rsiPeriod)
 *   - BUY  when RSI crosses below buyThreshold
 *   - SELL when RSI crosses above sellThreshold
 *
 * Searchable params:
 *   - rsiPeriod (int)
 *   - buyThreshold (int)  [0..100]
 *   - sellThreshold (int) [0..100]
 */
public class AlgoV7 {
    static final double INITIAL_INVESTMENT = 100;
    static final int BUY = 1;
    static final int SELL = -1;

    static String TOKEN;
    static String WORKING_DIRECTORY;
    static int DATA_RESOLUTION;
    static double FEE_RATE;
    static String FROM_DATE;
    static String TO_DATE;
    static TokenData ORIGINAL_DATA;
    static TokenData TOKEN_DATA;

    static public enum FitnessMethod {X_OVER_HODL, PROFIT, COMPOUND_RETURN_RATE}

    static FitnessMethod FITNESS_METHOD;
    static boolean GA_SEED;

    // Begin Agentic Workflow Section - 0. fixed variables
    // No fixed strategy params for this simplified concept.
    // End Agentic Workflow Section - 0. fixed variables

    // Begin Agentic Workflow Section - 1. input parameters
    static int rsiPeriod;
    static int buyThreshold;
    static int sellThreshold;
    // End Agentic Workflow Section - 1. input parameters

    /**
     * GA params.
     */
    static final int POPULATION_SIZE = 2000;
    static final int MAX_GENERATIONS = 800;
    static final int RUN_SIZE = 100;
    static final double CROSSOVER_PROBABILITY = 0.9;
    static final double MUTATION_PROBABILITY = 0.2;
    static final int ELITISM_COUNT = (int) Math.max(1, Math.round(POPULATION_SIZE * 0.02));

    static int INDICATOR_DIVIDER = 1; // not used (int params)

    // Begin Agentic Workflow Section - 2. bit positions
    static BitPositions bitPositions = BitPositions.assemble(
            // 6 bits -> 0..63 (guard to >=2)
            "RSI_PERIOD", 6,
            // 7 bits -> 0..127 (clamp to 0..100)
            "BUY_TH", 7,
            // 7 bits -> 0..127 (clamp to 0..100)
            "SELL_TH", 7
    );
    // End Agentic Workflow Section - 2. bit positions

    public static void main(String[] args) throws IOException {
        String choice = choicePrompt(args);

        if ("1".equals(choice)
                || "SCE".equalsIgnoreCase(choice)
                || (args.length > 0 && "SCE".equalsIgnoreCase(args[0]))) {
            singleCandidateEvaluation();
        } else if ("2".equals(choice)
                || "GA".equalsIgnoreCase(choice)
                || (args.length > 0 && "GA".equalsIgnoreCase(args[0]))) {
            geneticAlgorithmSearch();
        } else {
            System.out.println("Invalid choice. Please enter 1 (SCE) or 2 (GA).");
        }
    }

    // Begin Agentic Workflow Section - 3. genetic algorithm search
    private static void geneticAlgorithmSearch() throws IOException {
        TOKEN = "BTC";
//        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        // Seed params
        rsiPeriod = 14;
        buyThreshold = 30;
        sellThreshold = 70;

        GA_SEED = false;
        FITNESS_METHOD = FitnessMethod.X_OVER_HODL;

        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);

        CandidateFactory<BitString> candidateFactory = new BitStringFactory(bitPositions.getBitWidth());
        EvolutionaryOperator<BitString> pipeline = new EvolutionPipeline<>(Arrays.asList(
                new ProbabilisticCrossover(1, new Probability(CROSSOVER_PROBABILITY)),
                new BitStringMutation(new Probability(MUTATION_PROBABILITY))
        ));

        FitnessEvaluator<BitString> fitnessEvaluator = new FitnessFunction();
        ProgressListener progressListener = new ProgressListener();
        EvolutionEngine<BitString> engine = new GenerationalEvolutionEngine<>(
                candidateFactory,
                pipeline,
                fitnessEvaluator,
                new RouletteWheelSelection(),
                new Random()
        );

        engine.addEvolutionObserver(progressListener);
        StagnationTermination stagnationTermination = new StagnationTermination(RUN_SIZE);

        List<BitString> seedCandidates = generateSeedCandidates();

        long startTime = System.currentTimeMillis();
        System.out.println("GA start...");
        if (GA_SEED)
            engine.evolve(POPULATION_SIZE, ELITISM_COUNT, seedCandidates, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        else
            engine.evolve(POPULATION_SIZE, ELITISM_COUNT, new GenerationCount(MAX_GENERATIONS), stagnationTermination);
        System.out.println("GA end...\n");
        long endTime = System.currentTimeMillis();

        BitString bestSolution = progressListener.getBestCandidate();

        rsiPeriod = bitsToInt(bestSolution,
                bitPositions.get("RSI_PERIOD").getStartPos(),
                bitPositions.get("RSI_PERIOD").getEndPos());
        buyThreshold = clamp(bitsToInt(bestSolution,
                bitPositions.get("BUY_TH").getStartPos(),
                bitPositions.get("BUY_TH").getEndPos()), 0, 100);
        sellThreshold = clamp(bitsToInt(bestSolution,
                bitPositions.get("SELL_TH").getStartPos(),
                bitPositions.get("SELL_TH").getEndPos()), 0, 100);

        System.out.printf("Best solution (rsiPeriod, buyTh, sellTh): %d, %d, %d%n",
                rsiPeriod, buyThreshold, sellThreshold);
        System.out.printf("GA execution time: %d milliseconds%n", (endTime - startTime));

        double profit = executeStandardTradeRules("GA");
        int fromPos = rsiPeriod + 1;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        double xOverHodl = profit / hodlProfit;
        System.out.println("xOverHodl: " + xOverHodl + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
    }
    // End Agentic Workflow Section - 3. genetic algorithm search

    // Begin Agentic Workflow Section - 4. single candidate evaluation
    private static void singleCandidateEvaluation() throws IOException {
        TOKEN = "BTC";
//        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\kraken";
        WORKING_DIRECTORY = "C:\\work\\discovery-scripts\\data\\pdma\\binance";
        DATA_RESOLUTION = 15;
        FEE_RATE = 0.001;
        FROM_DATE = "2020-07-24";
        TO_DATE = "2025-07-30";

        rsiPeriod = 14;
        buyThreshold = 30;
        sellThreshold = 70;

        System.out.println("Begin Data Fetch: " + FROM_DATE);
        ORIGINAL_DATA = TokenDataFetcher.fetchTokenStockCloseDataPdma(FROM_DATE, TO_DATE, DATA_RESOLUTION, TOKEN, WORKING_DIRECTORY);
        System.out.println("End Data Fetch: " + TO_DATE);
        System.out.println(ORIGINAL_DATA.getTokenSymbol());

        double profit = executeStandardTradeRules("SCE");
        int fromPos = rsiPeriod + 1;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double hodlProfit = executeStandardModel(fromPos, toPos);
        double xOverHodl = profit / hodlProfit;
        System.out.println("xOverHodl: " + xOverHodl + " - profit: " + profit + " - hodlProfit: " + hodlProfit);
    }
    // End Agentic Workflow Section - 4. single candidate evaluation

    private static @NotNull String choicePrompt(String[] args) {
        String choice = "";
        if (args.length == 0) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Choose mode [1 = SCE (Single Candidate Evaluation), 2 = GA (Genetic Algorithm Search)]: ");
            choice = scanner.nextLine().trim();
        }
        return choice;
    }

    // Begin Agentic Workflow Section - 5. execute standard trading rules
    public static double executeStandardTradeRules(String type) {
        System.out.println("Generating signals");
        System.out.println("1: " + LocalDateTime.now());
        TokenData tokenData = generateCustomSignals(rsiPeriod, buyThreshold, sellThreshold);
        System.out.println("2: " + LocalDateTime.now());
        System.out.println("Custom signals complete\n");

        System.out.println("Calculating profit");
        System.out.println("3: " + LocalDateTime.now());
        int fromPos = rsiPeriod + 1;
        int toPos = ORIGINAL_DATA.getClose().length - 1;
        double profit = calculateProfit(fromPos, toPos, tokenData);
        System.out.println("4: " + LocalDateTime.now());
        System.out.println("Calculating profit complete");

        System.out.printf("\nInitial Investment in USD $: %.2f%n", INITIAL_INVESTMENT);
        System.out.printf("=> Profit in USD $: %.2f%n", profit);
        System.out.printf("=> Current Holdings in USD $: %.2f%n%n", profit + INITIAL_INVESTMENT);

        if (type.equals("SCE")) {
            TOKEN_DATA = tokenData;
            double crr = calculateCompoundedReturnRate(fromPos, toPos, tokenData);
            System.out.println("Compounded Return Rate: " + Math.max(crr, 0));
        }

        return profit;
    }
    // End Agentic Workflow Section - 5. execute standard trading rules

    // Begin Agentic Workflow Section - 6. generate custom signals
    public static TokenData generateCustomSignals(int rsiPeriod, int buyThreshold, int sellThreshold) {
        TokenData tokenData = new TokenData(ORIGINAL_DATA.getTokenSymbol(), ORIGINAL_DATA.getClose().length);

        double[] close = ORIGINAL_DATA.getClose();
        int n = close.length;

        double[] rsi = new double[n];
        double[] orderSignal = new double[n];
        Arrays.fill(rsi, 0.0);
        Arrays.fill(orderSignal, 0.0);

        // Wilder RSI
        double avgGain = 0.0;
        double avgLoss = 0.0;

        // initial period
        for (int i = 1; i <= rsiPeriod && i < n; i++) {
            double change = close[i] - close[i - 1];
            if (change > 0) avgGain += change;
            else avgLoss += -change;
        }
        if (rsiPeriod < n) {
            avgGain /= rsiPeriod;
            avgLoss /= rsiPeriod;
        }

        for (int i = rsiPeriod + 1; i < n; i++) {
            double change = close[i] - close[i - 1];
            double gain = Math.max(change, 0.0);
            double loss = Math.max(-change, 0.0);

            avgGain = ((avgGain * (rsiPeriod - 1)) + gain) / rsiPeriod;
            avgLoss = ((avgLoss * (rsiPeriod - 1)) + loss) / rsiPeriod;

            if (avgLoss == 0.0) {
                rsi[i] = 100.0;
            } else {
                double rs = avgGain / avgLoss;
                rsi[i] = 100.0 - (100.0 / (1.0 + rs));
            }
        }

        int start = rsiPeriod + 2;
        for (int i = start; i < n; i++) {
            double prev = rsi[i - 1];
            double cur = rsi[i];

            boolean buyCross = prev >= buyThreshold && cur < buyThreshold;
            boolean sellCross = prev <= sellThreshold && cur > sellThreshold;

            if (buyCross) orderSignal[i] = BUY;
            else if (sellCross) orderSignal[i] = SELL;
        }

        tokenData.getData().put("RSI", rsi);
        tokenData.getData().put("Order Signal", orderSignal);
        return tokenData;
    }
    // End Agentic Workflow Section - 6. generate custom signals

    // Begin Agentic Workflow Section - 7. calculate profit
    public static double calculateProfit(int fromPos, int toPos, TokenData tokenData) {
        double[] holdingsVector = new double[ORIGINAL_DATA.getClose().length];
        Arrays.fill(holdingsVector, 0.0);

        double currentHoldings = INITIAL_INVESTMENT;
        boolean inPosition = false;
        double tokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = Math.max(0, fromPos); i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) continue;

            if (orderSignal[i] == BUY && !inPosition) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                tokenAmount = currentHoldings / closePrice[i];
                inPosition = true;
                holdingsVector[i] = currentHoldings;

            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * closePrice[i];
                double fee = FEE_RATE * sellValue;
                sellValue -= fee;

                currentHoldings = sellValue;
                inPosition = false;
                tokenAmount = 0.0;
                holdingsVector[i] = currentHoldings;

            } else {
                holdingsVector[i] = inPosition ? tokenAmount * closePrice[i] : currentHoldings;
            }
        }

        tokenData.getData().put("Holdings", holdingsVector);

        double finalHoldings = inPosition ? tokenAmount * closePrice[toPos] : currentHoldings;
        return finalHoldings - INITIAL_INVESTMENT;
    }
    // End Agentic Workflow Section - 7. calculate profit

    public static double calculateCompoundedReturnRate(int fromPos, int toPos, TokenData tokenData) {
        double compoundedReturnRate = 0.0;
        boolean inPosition = false;
        double currentHoldings = INITIAL_INVESTMENT;
        double tokenAmount = 0.0;

        double[] orderSignal = tokenData.getData().get("Order Signal");
        double[] closePrice = ORIGINAL_DATA.getClose();

        for (int i = fromPos; i <= toPos; i++) {
            if (Double.isNaN(orderSignal[i])) continue;

            if (orderSignal[i] == BUY && !inPosition) {
                double fee = FEE_RATE * currentHoldings;
                currentHoldings -= fee;
                tokenAmount = currentHoldings / closePrice[i];
                inPosition = true;

            } else if (orderSignal[i] == SELL && inPosition) {
                double sellValue = tokenAmount * closePrice[i];
                double fee = FEE_RATE * sellValue;
                sellValue -= fee;

                double logReturn = Math.log(sellValue / currentHoldings);
                compoundedReturnRate += logReturn * 100.0;

                currentHoldings = sellValue;
                tokenAmount = 0.0;
                inPosition = false;
            }
        }

        return compoundedReturnRate;
    }

    public static double executeStandardModel(int fromPos, int toPos) {
        double beginPrice = ORIGINAL_DATA.getClose()[fromPos];
        double endPrice = ORIGINAL_DATA.getClose()[toPos];

        double differencePrice = endPrice - beginPrice;
        double percentageDifference = differencePrice / beginPrice;

        double profit = INITIAL_INVESTMENT * percentageDifference;
        double currentHoldings = profit + INITIAL_INVESTMENT;

        double fee = FEE_RATE * currentHoldings;
        currentHoldings -= fee;

        return profit;
    }

    private static int bitsToInt(BitString bitString, int start, int end) {
        int value = 0;
        for (int i = start; i < end; i++) {
            if (bitString.getBit(i)) {
                value |= 1 << (i - start);
            }
        }
        return value;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Begin Agentic Workflow Section - 8. generate seed candidates
    private static List<BitString> generateSeedCandidates() {
        BitString seed = new BitString(bitPositions.getBitWidth());

        intToBits(seed,
                bitPositions.get("RSI_PERIOD").getStartPos(),
                bitPositions.get("RSI_PERIOD").getEndPos(),
                rsiPeriod);

        intToBits(seed,
                bitPositions.get("BUY_TH").getStartPos(),
                bitPositions.get("BUY_TH").getEndPos(),
                buyThreshold);

        intToBits(seed,
                bitPositions.get("SELL_TH").getStartPos(),
                bitPositions.get("SELL_TH").getEndPos(),
                sellThreshold);

        System.out.println("SEED DEBUG:");
        System.out.printf("Input rsiPeriod = %d%n", rsiPeriod);
        System.out.printf("Input buyThreshold = %d%n", buyThreshold);
        System.out.printf("Input sellThreshold = %d%n", sellThreshold);

        return Collections.singletonList(seed);
    }
    // End Agentic Workflow Section - 8. generate seed candidates

    private static void intToBits(BitString bitString, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            int bitIndex = i - start;
            boolean bit = ((value >> bitIndex) & 1) == 1;
            bitString.setBit(i, bit);
        }
    }

    // Begin Agentic Workflow Section - 9. fitness function
    private static class FitnessFunction implements FitnessEvaluator<BitString> {
        @Override
        public double getFitness(BitString candidate, List<? extends BitString> population) {
            int period = bitsToInt(candidate,
                    bitPositions.get("RSI_PERIOD").getStartPos(),
                    bitPositions.get("RSI_PERIOD").getEndPos());

            int buy = clamp(bitsToInt(candidate,
                    bitPositions.get("BUY_TH").getStartPos(),
                    bitPositions.get("BUY_TH").getEndPos()), 0, 100);

            int sell = clamp(bitsToInt(candidate,
                    bitPositions.get("SELL_TH").getStartPos(),
                    bitPositions.get("SELL_TH").getEndPos()), 0, 100);

            int fromPos = period + 2;

            // Guards: RSI needs period>=2 and sensible thresholds.
            if (period < 2 || fromPos >= ORIGINAL_DATA.getClose().length) return 0;
            if (buy >= sell) return 0; // enforce classic buy low / sell high
            if (buy < 5 || sell > 95) return 0; // avoid degenerate always-in/out

            TokenData tokenData = generateCustomSignals(period, buy, sell);
            int toPos = ORIGINAL_DATA.getClose().length - 1;

            double profit = calculateProfit(fromPos, toPos, tokenData);
            double hodlProfit = executeStandardModel(fromPos, toPos);

            double fitnessScore = 0;
            if (FITNESS_METHOD == FitnessMethod.X_OVER_HODL)
                fitnessScore = profit / hodlProfit;
            if (FITNESS_METHOD == FitnessMethod.PROFIT)
                fitnessScore = profit;
            if (FITNESS_METHOD == FitnessMethod.COMPOUND_RETURN_RATE)
                fitnessScore = calculateCompoundedReturnRate(fromPos, toPos, tokenData);

            return Math.max(fitnessScore, 0);
        }

        @Override
        public boolean isNatural() {
            return true;
        }
    }
    // End Agentic Workflow Section - 9. fitness function

    private static class ProbabilisticCrossover implements EvolutionaryOperator<BitString> {
        private final BitStringCrossover crossoverOperator;
        private final Probability probability;

        public ProbabilisticCrossover(int crossoverPoints, Probability probability) {
            this.crossoverOperator = new BitStringCrossover(crossoverPoints);
            this.probability = probability;
        }

        @Override
        public List<BitString> apply(List<BitString> population, Random rng) {
            if (rng.nextDouble() < probability.doubleValue()) {
                return crossoverOperator.apply(population, rng);
            }
            return population;
        }
    }

    private static class StagnationTermination implements TerminationCondition {
        private final int maxGenerationsWithoutImprovement;
        private double lastBestFitness = Double.NEGATIVE_INFINITY;
        private int stagnantGenerations = 0;

        public StagnationTermination(int maxGenerationsWithoutImprovement) {
            this.maxGenerationsWithoutImprovement = maxGenerationsWithoutImprovement;
        }

        @Override
        public boolean shouldTerminate(PopulationData<?> populationData) {
            double currentBestFitness = populationData.getBestCandidateFitness();
            if (currentBestFitness > lastBestFitness) {
                lastBestFitness = currentBestFitness;
                stagnantGenerations = 0;
            } else {
                stagnantGenerations++;
            }
            return stagnantGenerations >= maxGenerationsWithoutImprovement;
        }
    }

    // Begin Agentic Workflow Section - 10. progress listener
    private static class ProgressListener implements EvolutionObserver<BitString> {
        private BitString bestCandidate;

        @Override
        public void populationUpdate(PopulationData<? extends BitString> data) {
            bestCandidate = data.getBestCandidate();

            int period = bitsToInt(bestCandidate,
                    bitPositions.get("RSI_PERIOD").getStartPos(),
                    bitPositions.get("RSI_PERIOD").getEndPos());

            int buy = clamp(bitsToInt(bestCandidate,
                    bitPositions.get("BUY_TH").getStartPos(),
                    bitPositions.get("BUY_TH").getEndPos()), 0, 100);

            int sell = clamp(bitsToInt(bestCandidate,
                    bitPositions.get("SELL_TH").getStartPos(),
                    bitPositions.get("SELL_TH").getEndPos()), 0, 100);

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm:ss a yyyy");
            String formattedDate = now.format(formatter);

            System.out.printf(
                    "Generation %d: Best fitness: %.3f, Solution (rsiPeriod, buyTh, sellTh): %d, %d, %d - %s%n",
                    data.getGenerationNumber(),
                    data.getBestCandidateFitness(),
                    period,
                    buy,
                    sell,
                    formattedDate
            );
        }

        public BitString getBestCandidate() {
            return bestCandidate;
        }
    }
    // End Agentic Workflow Section - 10. progress listener

    private static class BitStringFactory implements CandidateFactory<BitString> {
        private final int length;

        public BitStringFactory(int length) {
            this.length = length;
        }

        @Override
        public BitString generateRandomCandidate(Random rng) {
            return new BitString(length, rng);
        }

        @Override
        public List<BitString> generateInitialPopulation(int populationSize, Random rng) {
            List<BitString> population = new ArrayList<>(populationSize);
            for (int i = 0; i < populationSize; i++) {
                population.add(generateRandomCandidate(rng));
            }
            return population;
        }

        @Override
        public List<BitString> generateInitialPopulation(int populationSize, Collection<BitString> seedCandidates, Random rng) {
            List<BitString> population = new ArrayList<>(populationSize);
            population.addAll(seedCandidates);
            for (int i = seedCandidates.size(); i < populationSize; i++) {
                population.add(generateRandomCandidate(rng));
            }
            return population;
        }
    }

    public static final class BitPositions {

        public static final class BitField {
            private final int length;
            private final int startPos;
            private final int endPos;

            public BitField(int length, int startPos) {
                this.length = length;
                this.startPos = startPos;
                this.endPos = startPos + length;
            }

            public int getLength() {
                return length;
            }

            public int getStartPos() {
                return startPos;
            }

            public int getEndPos() {
                return endPos;
            }
        }

        private final LinkedHashMap<String, BitField> fields;
        private final int bitWidth;

        private BitPositions(LinkedHashMap<String, BitField> fields, int bitWidth) {
            this.fields = fields;
            this.bitWidth = bitWidth;
        }

        public static BitPositions assemble(Object... kv) {
            if (kv.length % 2 != 0) {
                throw new IllegalArgumentException("Must pass even number of arguments: key, value, key, value...");
            }

            LinkedHashMap<String, BitField> m = new LinkedHashMap<>();
            int cursor = 0;

            for (int i = 0; i < kv.length; i += 2) {
                Object k = kv[i];
                Object v = kv[i + 1];

                if (!(k instanceof String)) {
                    throw new IllegalArgumentException("Key at index " + i + " is not a String: " + k);
                }
                if (!(v instanceof Integer)) {
                    throw new IllegalArgumentException("Value at index " + (i + 1) + " is not an Integer: " + v);
                }

                String key = (String) k;
                int length = (Integer) v;

                if (length < 0) {
                    throw new IllegalArgumentException("Length for key '" + key + "' must be >= 0, got: " + length);
                }
                if (m.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate key: " + key);
                }

                BitField field = new BitField(length, cursor);
                m.put(key, field);
                cursor = field.getEndPos();
            }

            return new BitPositions(m, cursor);
        }

        public BitField get(String key) {
            return fields.get(key);
        }

        public int getBitWidth() {
            return bitWidth;
        }
    }
}
