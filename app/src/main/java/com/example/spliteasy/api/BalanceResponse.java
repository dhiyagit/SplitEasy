package com.example.spliteasy.api;

import java.util.List;

public class BalanceResponse {
    private List<MemberBalance> memberBalances;
    private List<SuggestedDebt> suggestedSettlements;

    public List<MemberBalance> getMemberBalances() { return memberBalances; }
    public List<SuggestedDebt> getSuggestedSettlements() { return suggestedSettlements; }
}
