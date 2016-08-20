package org.camunda.bpm.scenarios.waitstate;


import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.scenarios.Scenario;
import org.camunda.bpm.scenarios.WaitstateAction;

import java.util.Map;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
public class ReceiveTaskWaitstate extends MessageIntermediateCatchEventWaitstate {

  protected ReceiveTaskWaitstate(ProcessEngine processEngine, HistoricActivityInstance instance) {
    super(processEngine, instance);
  }

  @Override
  protected WaitstateAction action(Scenario scenario) {
    return scenario.atReceiveTask(getActivityId());
  }

  @Override
  public void receiveMessage() {
    super.receiveMessage();
  }

  @Override
  public void receiveMessage(Map<String, Object> variables) {
    super.receiveMessage(variables);
  }

}
