package com.example.spliteasy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    public static class ReceiptItem {
        public String name;
        public double price;

        public ReceiptItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    public static class ReceiptData {
        public String merchantName = "Scanned Expense";
        public double subtotal = 0.0;
        public double tax = 0.0;
        public double grandTotal = 0.0;
        public List<ReceiptItem> items = new ArrayList<>();
    }

    public static ReceiptData parse(String ocrText) {
        ReceiptData data = new ReceiptData();
        if (ocrText == null || ocrText.isEmpty()) return data;

        String[] lines = ocrText.split("\n");
        List<String> validLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                validLines.add(trimmed);
            }
        }

        if (!validLines.isEmpty()) {
            data.merchantName = validLines.get(0);
        }

        Pattern pricePattern = Pattern.compile("(?:₹|Rs\\.?\\s*)?(\\d{1,3}(?:,\\d{3})*\\.\\d{2}|\\d+\\.\\d{2}|\\d+)");

        for (int i = 0; i < validLines.size(); i++) {
            String line = validLines.get(i);
            String upper = line.toUpperCase();

            if (upper.contains("GSTIN") || upper.contains("PHONE") || upper.contains("TEL") || upper.contains("INVOICE") || upper.contains("BILL NO")) {
                continue;
            }

            Matcher matcher = pricePattern.matcher(line);
            double foundPrice = -1.0;
            String matchedStr = "";
            while (matcher.find()) {
                try {
                    String priceStr = matcher.group(1).replace(",", "");
                    foundPrice = Double.parseDouble(priceStr);
                    matchedStr = matcher.group(0);
                } catch (Exception ignored) {}
            }

            if (foundPrice >= 0) {
                if (upper.contains("GRAND TOTAL") || upper.contains("TOTAL AMT") || upper.contains("AMOUNT PAYABLE") || (upper.contains("TOTAL") && !upper.contains("SUB"))) {
                    data.grandTotal = foundPrice;
                } else if (upper.contains("SUBTOTAL") || upper.contains("SUB TOTAL")) {
                    data.subtotal = foundPrice;
                } else if (upper.contains("TAX") || upper.contains("GST") || upper.contains("VAT") || upper.contains("CGST") || upper.contains("SGST")) {
                    data.tax += foundPrice;
                } else {
                    String itemName = line.replace(matchedStr, "").trim();
                    if (itemName.isEmpty() && i > 0) {
                        itemName = validLines.get(i - 1);
                    }
                    if (!itemName.isEmpty() && foundPrice > 0) {
                        data.items.add(new ReceiptItem(itemName, foundPrice));
                    }
                }
            }
        }

        if (data.grandTotal <= 0) {
            double itemSum = 0.0;
            for (ReceiptItem item : data.items) {
                itemSum += item.price;
            }
            data.grandTotal = itemSum > 0 ? itemSum + data.tax : data.subtotal + data.tax;
        }

        return data;
    }
}
