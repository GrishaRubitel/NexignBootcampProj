package com.bootcamp_proj.bootcampproj.psql_transactions;
import jakarta.persistence.*;

/**
 * ORM сущность для работы с таблицей "transactions" базы данных генератора звонков
 */
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

    public String getCallId() {
        return callId;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public long getMsisdnTo() {
        return msisdnTo;
    }

    public int getUnixStart() {
        return unixStart;
    }

    public int getUnixEnd() {
        return unixEnd;
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