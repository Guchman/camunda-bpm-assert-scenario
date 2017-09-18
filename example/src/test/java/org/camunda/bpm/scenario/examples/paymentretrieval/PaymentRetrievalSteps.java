package org.camunda.bpm.scenario.examples.paymentretrieval;

import cucumber.api.java8.En;
import org.camunda.bpm.engine.impl.test.TestHelper;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.camunda.bpm.scenario.delegate.EventSubscriptionDelegate;
import org.camunda.bpm.scenario.delegate.ExternalTaskDelegate;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.mockito.Mockito.*;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
public class PaymentRetrievalSteps implements En {

    private ProcessScenario paymentRetrieval;

    private Map<String, Object> initialVariables;

    private ProcessInstance instance;

    public PaymentRetrievalSteps() {

        Given("The customer has a start balance of (\\d+)", (Integer startBalance) -> {
            initialVariables.put("balance", startBalance);
        });

        Given("A payment of (\\d+) is required", (Integer paymentAmount) -> {
            initialVariables.put("paymentAmount", paymentAmount);
        });

        Given("The customer's credit card is already expired", () -> {
            when(paymentRetrieval.waitsAtServiceTask("chargeCard")).thenReturn((task) -> {
                task.handleBpmnError("creditCardExpired");
            }).thenReturn(ExternalTaskDelegate::complete);
        });

        Given("The customer does not update his credit card info in due time", () -> {
            when(paymentRetrieval.waitsAtReceiveTask("updateCard")).thenReturn((task) -> {
                task.defer("P14D", task::receive);
            });
        });

        When("We walk the customer through the payment retrieval process", () -> {
            Scenario scenario = Scenario.run(paymentRetrieval).startByKey("paymentRetrieval", initialVariables).execute();
            instance = scenario.instance(paymentRetrieval);
        });

        Then("The payment retrieval is successful: (true|false)", (Boolean paymentSuccessful) -> {
            assertThat(instance).hasPassed(paymentSuccessful ? "paymentReceived" : "paymentFailed");
        });

        Given("The customer has a final balance of (\\d+)", (Integer finalBalance) -> {
            assertThat(instance).variables().containsEntry("balance", finalBalance);
        });

    }

    {

        init(TestHelper.getProcessEngine("camunda.cfg.xml"));

        repositoryService()
                .createDeployment()
                .addClasspathResource("org/camunda/bpm/scenario/examples/paymentretrieval/PaymentRetrieval.bpmn")
                .deploy();

        initialVariables = new HashMap<>();

        paymentRetrieval = Mockito.mock(ProcessScenario.class);

        when(paymentRetrieval.waitsAtServiceTask("useCredit")).thenReturn((task) -> {

            Integer balance = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "balance");
            Integer paymentAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "paymentAmount");

            Integer chargeAmount = balance - paymentAmount < 0 ? paymentAmount - balance : 0;
            Integer newBalance = balance - paymentAmount + chargeAmount;

            task.complete(withVariables("chargeAmount", chargeAmount, "balance", newBalance));

        });

        when(paymentRetrieval.waitsAtServiceTask("chargeCard")).thenReturn(ExternalTaskDelegate::complete);

        when(paymentRetrieval.waitsAtReceiveTask("updateCard")).thenReturn(EventSubscriptionDelegate::receive);

        when(paymentRetrieval.waitsAtServiceTask("restoreCredit")).thenReturn((task) -> {

            Integer paymentAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "paymentAmount");
            Integer chargeAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "chargeAmount");

            Integer newBalance = paymentAmount - chargeAmount;

            task.complete(withVariables("balance", newBalance));

        });

    }

}
