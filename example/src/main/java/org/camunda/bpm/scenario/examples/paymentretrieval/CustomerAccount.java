package org.camunda.bpm.scenario.examples.paymentretrieval;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
public class CustomerAccount {

    private int balance;

    public CustomerAccount(int balance) {
        this.balance = balance;
    }

    public int withdraw(int amount) {
        if (amount < 0) throw new IllegalArgumentException();
        int withdraw = amount <= balance ? amount : balance;
        balance -= withdraw;
        return withdraw;
    }

    public void deposit(int amount) {
        if (amount < 0) throw new IllegalArgumentException();
        balance += amount;
    }

    public int getBalance() {
        return balance;
    }

}
