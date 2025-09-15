package com.banking.cli;

import java.io.Console;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * InputReader - helper for reading and validating console input.
 *
 * Responsibilities:
 *  - Centralize reading lines and masked passwords (Console or fallback to Scanner)
 *  - Provide safe parsing helpers (int, BigDecimal)
 *  - Offer confirmation prompts and simple list selection helpers
 *  - Normalize common commands (back/quit)
 *
 * Usage:
 *  InputReader in = new InputReader();
 *  String name = in.readLine("Enter name");
 *  BigDecimal amount = in.readAmount("Amount to withdraw");
 *  boolean ok = in.confirm("Proceed with withdrawal?");
 *  in.close();
 */
public class InputReader {

    private final Scanner scanner;
    private final Console console;
    private final Locale locale;

    // Common tokens the app will treat specially
    public static final String BACK_TOKEN = "b";
    public static final String BACK_TOKEN_LONG = "back";
    public static final String QUIT_TOKEN = "q";
    public static final String QUIT_TOKEN_LONG = "quit";

    public InputReader() {
        this(Locale.getDefault());
    }

    public InputReader(Locale locale) {
        this.locale = Objects.requireNonNull(locale);
        this.scanner = new Scanner(System.in);
        this.console = System.console();
    }

    /**
     * Read a line of text (trims result). Returns null if EOF reached.
     * If allowEmpty is false the method will keep prompting until non-empty input.
     */
    public String readLine(String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(prompt + ": ");
            String line = null;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                return null; // EOF or closed
            }
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty() && !allowEmpty) {
                System.out.println("Input cannot be empty. Try again or type 'b' to go back.");
                continue;
            }
            return line;
        }
    }

    /** Convenience: readLine with non-empty requirement. */
    public String readLine(String prompt) {
        return readLine(prompt, false);
    }

    /** Read a masked password. Uses System.console() if available; otherwise falls back to scanner (unmasked) and prints a short warning. */
    public String readPassword(String prompt) {
        if (console != null) {
            char[] chars = console.readPassword(prompt + ": ");
            if (chars == null) return null;
            return new String(chars);
        }
        // Fallback
        System.out.println("(Warning: masking unavailable in this environment — input will be visible)");
        return readLine(prompt, false);
    }

    /**
     * Read an integer within optional bounds. Use min>max to indicate no upper bound.
     * Returns null if EOF reached. Throws no exceptions; keeps prompting until valid or back/quit tokens entered.
     */
    public Integer readInt(String prompt, Integer min, Integer max) {
        while (true) {
            String in = readLine(prompt, false);
            if (in == null) return null;
            if (isBackOrQuit(in)) return null;
            try {
                int v = Integer.parseInt(in);
                if (min != null && v < min) {
                    System.out.println("Value must be >= " + min);
                    continue;
                }
                if (max != null && max >= min && v > max) {
                    System.out.println("Value must be <= " + max);
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer. Please enter a number.");
            }
        }
    }

    /** Read a BigDecimal amount (monetary). Accepts commas and locale-specific grouping. */
    public BigDecimal readAmount(String prompt) {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setGroupingUsed(true);
        while (true) {
            String in = readLine(prompt + " (e.g. 1500 or 1,500.00)", false);
            if (in == null) return null;
            if (isBackOrQuit(in)) return null;
            // Remove common currency symbols and whitespace
            String normalized = in.replaceAll("[Rr\\$€£\\s]", "");
            // Try parse with NumberFormat first
            try {
                Number parsed = nf.parse(normalized);
                BigDecimal bd = new BigDecimal(parsed.toString());
                // Scale to 2 decimals for money
                bd = bd.setScale(2, BigDecimal.ROUND_HALF_EVEN);
                if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Amount must be greater than zero.");
                    continue;
                }
                return bd;
            } catch (ParseException | NumberFormatException e) {
                // Try stripping commas/dots heuristically
                String digitsOnly = normalized.replaceAll("[^0-9.]", "");
                try {
                    BigDecimal bd = new BigDecimal(digitsOnly);
                    bd = bd.setScale(2, BigDecimal.ROUND_HALF_EVEN);
                    if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                        System.out.println("Amount must be greater than zero.");
                        continue;
                    }
                    return bd;
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid amount format. Try again.");
                }
            }
        }
    }

    /** Simple yes/no confirmation. Returns true for yes, false for no. Default is no. */
    public boolean confirm(String prompt) {
        while (true) {
            String in = readLine(prompt + " (y/n)", false);
            if (in == null) return false;
            in = in.trim().toLowerCase();
            if (isBackOrQuit(in)) return false;
            if (in.equals("y") || in.equals("yes")) return true;
            if (in.equals("n") || in.equals("no")) return false;
            System.out.println("Please answer 'y' or 'n'.");
        }
    }

    /** Choose an item from a list by index. Returns null if back/quit chosen. */
    public <T> T chooseFromList(List<T> items, Function<T, String> display, String prompt) {
        if (items == null || items.isEmpty()) return null;
        while (true) {
            for (int i = 0; i < items.size(); i++) {
                System.out.println("[" + (i + 1) + "] " + display.apply(items.get(i)));
            }
            System.out.println("b) Back");
            String sel = readLine(prompt, false);
            if (sel == null) return null;
            if (isBackOrQuit(sel)) return null;
            try {
                int idx = Integer.parseInt(sel.trim()) - 1;
                if (idx >= 0 && idx < items.size()) return items.get(idx);
                System.out.println("Selection out of range.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid selection. Enter the number of the item.");
            }
        }
    }

    /** Validate input against a predicate; keeps prompting until valid. */
    public String readValidated(String prompt, Predicate<String> validator, String errorMessage) {
        while (true) {
            String in = readLine(prompt, false);
            if (in == null) return null;
            if (isBackOrQuit(in)) return null;
            if (validator.test(in)) return in;
            System.out.println(errorMessage);
        }
    }

    public boolean isBackOrQuit(String input) {
        if (input == null) return false;
        String t = input.trim().toLowerCase();
        return t.equals(BACK_TOKEN) || t.equals(BACK_TOKEN_LONG) || t.equals(QUIT_TOKEN) || t.equals(QUIT_TOKEN_LONG);
    }

    public void close() {
        try {
            scanner.close();
        } catch (Exception ignored) {}
    }
}

