Feature: Restore a credit withdrawn for a failing payment

  As a customer I expect that my credit, which was withdrawn
  for a payment exceeding that credit, is restored again in
  case the remaining payment fails, so that I can use my
  credit again for later payments.

  Scenario Outline: Restore account balance when payment ultimately fails

    Given the customer has an account balance of <start>
    And   a payment of <amount> is required
    But   the customer's credit card already expired
    And   the customer does not update his credit card for 14 days and 1 hours
    When  we walk the customer through the payment retrieval
    Then  the payment retrieval <is_not> successful
    And   the customer has a final balance of <final>

    Examples:
      | start | amount  | is_not | final |
      | 50    | 70      | is not | 50    |
      | 50    | 50      | is     | 0     |
      | 50    | 30      | is     | 20    |
