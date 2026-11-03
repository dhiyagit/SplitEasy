package com.example.spliteasy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {
    public static String format(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setCurrencySymbol("₹");
        DecimalFormat formatter = new DecimalFormat("₹#,##0.00", symbols);
        return formatter.format(amount);
    }
}
