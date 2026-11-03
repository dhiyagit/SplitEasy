package com.example.spliteasy.api;

import com.example.spliteasy.CurrencyUtils;

public class MemberBalance {
    private Long userId;
    private String fullName;
    private String email;
    private Double totalPaid;
    private Double totalShare;
    private Double netBalance;

    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Double getTotalPaid() { return totalPaid; }
    public Double getTotalShare() { return totalShare; }
    public Double getNetBalance() { return netBalance; }

    public String getFormattedText() {
        double paid = totalPaid != null ? totalPaid : 0.0;
        double share = totalShare != null ? totalShare : 0.0;
        double net = netBalance != null ? netBalance : 0.0;

        String formattedPaid = CurrencyUtils.format(paid);
        String formattedShare = CurrencyUtils.format(share);

        if (Math.abs(net) < 0.01) {
            return fullName + "\nPaid: " + formattedPaid + " | Share: " + formattedShare + "\nStatus: Settled — " + CurrencyUtils.format(0.0);
        } else if (net > 0) {
            return fullName + "\nPaid: " + formattedPaid + " | Share: " + formattedShare + "\nStatus: Gets back +" + CurrencyUtils.format(net);
        } else {
            return fullName + "\nPaid: " + formattedPaid + " | Share: " + formattedShare + "\nStatus: Owes -" + CurrencyUtils.format(Math.abs(net));
        }
    }

    @Override
    public String toString() {
        return getFormattedText();
    }
}
