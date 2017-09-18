Feature: Restore credits reserved for failing fulfillments

  As a customer I expect a positive account balance, which is
  reserved for an order, to be freed up again in case order
  fulfillment fails, so that I can use my account balance for
  further orders.

  Scenario Outline: Restore reserved credits when payment
                    fails for expired credit cards and
                    custumer does not provide new infos

    Given The customer has a start balance of <startBalance>
    And   A payment of <paymentAmount> is required
    And   The customer's credit card is already expired
    But   The customer does not update his credit card info in due time
    When  We walk the customer through the payment retrieval process
    Then  The payment retrieval is successful: <paymentSuccessful>
    And   The customer has a final balance of <finalBalance>

    Examples:
      | startBalance | paymentAmount | paymentSuccessful | finalBalance |
      | 50           | 70            | false             | 50           |
      | 50           | 50            | true              | 0            |
      | 50           | 30            | true              | 20           |
