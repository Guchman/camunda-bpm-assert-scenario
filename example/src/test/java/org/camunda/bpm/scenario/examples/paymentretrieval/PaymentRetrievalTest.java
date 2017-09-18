package org.camunda.bpm.scenario.examples.paymentretrieval;

import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareAssertions;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.scenario.ProcessScenario;
import org.camunda.bpm.scenario.Scenario;
import org.camunda.bpm.scenario.delegate.EventSubscriptionDelegate;
import org.camunda.bpm.scenario.delegate.ExternalTaskDelegate;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.withVariables;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Deployment(resources = {
  "org/camunda/bpm/scenario/examples/paymentretrieval/PaymentRetrieval.bpmn"
})
public class PaymentRetrievalTest {

  @Rule
  @ClassRule
  public static ProcessEngineRule rule =
      TestCoverageProcessEngineRuleBuilder.create()
        .withDetailedCoverageLogging().build();

  // Mock all waitstates in main process and call activity with a scenario
  @Mock private ProcessScenario paymentRetrieval;
  private Map<String, Object> variables;

  // Setup a default behaviour for all "completable" waitstates in your
  // processes. You might want to override the behaviour in test methods.
  @Before
  public void setupDefaultScenario() {

    MockitoAnnotations.initMocks(this);

    variables = new HashMap<>();
    variables.put("balance", 50);

    when(paymentRetrieval.waitsAtServiceTask("useCredit")).thenReturn((task) -> {

      Integer balance = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "balance");
      Integer paymentAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "paymentAmount");

      Integer chargeAmount = balance - paymentAmount < 0 ? paymentAmount - balance : 0;
      Integer newBalance = balance - paymentAmount + chargeAmount;

      task.complete(withVariables("chargeAmount", chargeAmount, "balance", newBalance));

    });

    when(paymentRetrieval.waitsAtServiceTask("chargeCard")).thenReturn((task) -> {
      task.handleBpmnError("creditCardExpired");
    }).thenReturn(ExternalTaskDelegate::complete);

    when(paymentRetrieval.waitsAtReceiveTask("updateCard")).thenReturn((task) -> {
      task.defer("P14D", task::receive);
    });

    when(paymentRetrieval.waitsAtServiceTask("restoreCredit")).thenReturn((task) -> {

      Integer paymentAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "paymentAmount");
      Integer chargeAmount = (Integer) runtimeService().getVariable(task.getProcessInstanceId(), "chargeAmount");

      Integer newBalance = paymentAmount - chargeAmount;

      task.complete(withVariables("balance", newBalance));

    });

  }

  @Test
  public void testCharge70() {

    variables.put("paymentAmount", 70);

    Scenario scenario = Scenario.run(paymentRetrieval)
        .startByKey("paymentRetrieval", variables) // either just start process by key ...
        .execute();

    assertThat(scenario.instance(paymentRetrieval)).hasPassed("paymentFailed");
    assertThat(scenario.instance(paymentRetrieval)).variables().containsEntry("balance", 50);

  }

  @Test
  public void testCharge50() {

    variables.put("paymentAmount", 50);

    Scenario scenario = Scenario.run(paymentRetrieval)
            .startByKey("paymentRetrieval", variables) // either just start process by key ...
            .execute();

    assertThat(scenario.instance(paymentRetrieval)).hasPassed("paymentReceived");
    assertThat(scenario.instance(paymentRetrieval)).variables().containsEntry("balance", 0);

  }

  @Test
  public void testCharge30() {

    variables.put("paymentAmount", 30);

    Scenario scenario = Scenario.run(paymentRetrieval)
            .startByKey("paymentRetrieval", variables) // either just start process by key ...
            .execute();

    assertThat(scenario.instance(paymentRetrieval)).hasPassed("paymentReceived");
    assertThat(scenario.instance(paymentRetrieval)).variables().containsEntry("balance", 20);

  }

  @Test
  public void testParsingAndDeployment() {
    // nothing is done here, as we just want to check for exceptions during deployment
  }

}
