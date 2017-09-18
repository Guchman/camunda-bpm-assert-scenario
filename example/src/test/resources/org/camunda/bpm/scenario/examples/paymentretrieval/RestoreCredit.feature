Feature: Restore a credit withdrawn for a failing payment

  As a customer I expect that my credit, which was withdrawn
  for a payment exceeding that credit, is restored again in
  case the remaining payment fails, so that I can use my
  credit again for later payments.

  Scenario Outline: Restore account balance when a payment
  fails for an expired credit card and custumer does not
  provide new card details.

    Given the customer has an account balance of <start>
    And   a payment of <payment> is required
    And   the customer's credit card already expired
    But   the customer does not update his credit card for 7 days and 1 hours
    When  we walk the customer through the payment retrieval
    Then  the payment retrieval is <not> successful
    And   the customer has a final balance of <final>

    Examples:
      | start | payment | not | final |
      | 50    | 70      | not | 50    |
      | 50    | 50      |     | 0     |
      | 50    | 30      |     | 20    |
