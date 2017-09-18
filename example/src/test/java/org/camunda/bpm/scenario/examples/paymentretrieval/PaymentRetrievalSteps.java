package org.camunda.bpm.scenario.examples.paymentretrieval;

import cucumber.api.java8.En;
import org.camunda.bpm.engine.impl.test.TestHelper;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.camunda.bpm.scenario.delegate.EventSubscriptionDelegate;
import org.camunda.bpm.scenario.delegate.ExternalTaskDelegate;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.mockito.Mockito.*;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
public class PaymentRetrievalSteps implements En {

    private CustomerAccount customerAccount;
    private Integer paymentAmount;

    private ProcessScenario paymentRetrieval = mock(ProcessScenario.class);;
    private ProcessInstance instance;

    public PaymentRetrievalSteps() {

        // step definitions for cucumber

        Given("the customer has an account balance of (\\d+)", (Integer startBalance) -> {
            customerAccount = new CustomerAccount(startBalance);
        });

        Given("a payment of (\\d+) is required", (Integer paymentAmount) -> {
            this.paymentAmount = paymentAmount;
        });

        Given("the customer's credit card already expired", () -> {
            when(paymentRetrieval.waitsAtServiceTask("chargeCard")).thenReturn((task) -> {
                task.handleBpmnError("creditCardExpired");
            }).thenReturn(ExternalTaskDelegate::complete);
        });

        Given("the customer does not update his credit card for (\\d+) days and (\\d+) hours", (Integer days, Integer hours) -> {
            when(paymentRetrieval.waitsAtReceiveTask("updateCard")).thenReturn((task) -> {
                task.defer("P" + days + "DT" + hours + "H", task::receive);
            });
        });

        When("we walk the customer through the payment retrieval", () -> {
            Scenario scenario = Scenario.run(paymentRetrieval)
                    .startByKey("paymentRetrieval",
                            withVariables("paymentAmount", paymentAmount))
                    .execute();
            instance = scenario.instance(paymentRetrieval);
        });

        Then("the payment retrieval (is|is\\snot) successful", (String paymentSuccessful) -> {
            assertThat(instance)
                    .hasPassed(paymentSuccessful.equals("is") ? "paymentReceived" : "paymentFailed");
        });

        Given("the customer has a final balance of (\\d+)", (Integer finalBalance) -> {
            assertThat(customerAccount.getBalance())
                    .isEqualTo(finalBalance);
        });

    }

    {

        // quick'n'dirty ramp up of engine - should be refactored when doing more

        init(TestHelper.getProcessEngine("cucumber.cfg.xml"));

        repositoryService().createDeployment()
                .addClasspathResource("org/camunda/bpm/scenario/examples/paymentretrieval/PaymentRetrieval.bpmn")
                .deploy();

        // defining default behaviour for all steps / waitstates enables us to only declare
        // interesting steps differing from the normal in the feature files. Furthermore,
        // changing the process in a "backwards compatible" way - e.g. by adding more nodes
        // typically won't break existing feature scenarios.

        when(paymentRetrieval.waitsAtServiceTask("useCredit")).thenReturn((task) -> {
            int paymentAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "paymentAmount");
            int withdrawAmount = customerAccount.withdraw(paymentAmount);
            task.complete(withVariables("chargeAmount", paymentAmount - withdrawAmount));
        });

        when(paymentRetrieval.waitsAtServiceTask("chargeCard")).thenReturn(ExternalTaskDelegate::complete);

        when(paymentRetrieval.waitsAtReceiveTask("updateCard")).thenReturn(EventSubscriptionDelegate::receive);

        when(paymentRetrieval.waitsAtServiceTask("restoreCredit")).thenReturn((task) -> {
            int paymentAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "paymentAmount");
            int chargeAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "chargeAmount");
            customerAccount.deposit(paymentAmount - chargeAmount);
            task.complete();
        });

    }

}
