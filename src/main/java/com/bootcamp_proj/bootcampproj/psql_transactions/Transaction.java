package com.bootcamp_proj.bootcampproj.psql_transactions;
import jakarta.persistence.*;

@Entity
@Table(name="transactions")
public class Transaction {
    protected static final String IN_BREAK = ", ";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long transactionId;
    protected long msisdn;
    protected long msisdnTo;
    protected String callId;
    protected int unixStart;
    protected int unixEnd;

    public Transaction() {}

    public long getMsisdn() {
        return msisdn;
    }

    public long getMsisdnTo() {
        return msisdnTo;
    }

    public String getCallId() {
        return callId;
    }

    public int getUnixStart() {
        return unixStart;
    }

    public int getUnixEnd() {
        return unixEnd;
    }

    public Transaction(String rec) {
        String[] split = rec.split(IN_BREAK);

        callId = split[0];
        msisdn = Long.parseLong(split[1]);
        msisdnTo = Long.parseLong(split[2]);
        unixStart = Integer.parseInt(split[3]);
        unixEnd = Integer.parseInt(split[4]);
    }

    public Transaction(long msisdn, long msisdnTo, String callId, int unixStart, int unixEnd) {
        this.msisdn = msisdn;
        this.msisdnTo = msisdnTo;
        this.callId = callId;
        this.unixStart = unixStart;
        this.unixEnd = unixEnd;
    }

    @Override
    public String toString() {
        return transactionId + IN_BREAK + callId + IN_BREAK + msisdn + IN_BREAK + msisdnTo + IN_BREAK +
                unixStart + IN_BREAK + unixEnd;
    }
}