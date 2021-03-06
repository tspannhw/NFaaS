package com.hortonworks.faas.nfaas.core.helper;

import com.hortonworks.faas.nfaas.config.EntityState;
import org.apache.nifi.web.api.entity.ProcessGroupEntity;
import org.apache.nifi.web.api.entity.ProcessGroupFlowEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
public class ProcessorGroupFlowFacadeHelper extends  BaseFacadeHelper{

    public static final Logger logger = LoggerFactory.getLogger(ProcessGroupFacadeHelper.class);

    @Autowired
    ProcessGroupFacadeHelper processGroupFacadeHelper;
    /**
     * Call the NIFI rest api to stop the process group
     *
     * @param processGroupFlowEntity
     * @param state
     */
    public void stopProcessGroupComponents(ProcessGroupFlowEntity processGroupFlowEntity, String state) {
        processGroupFlow.startOrStopProcessGroupComponents(processGroupFlowEntity, state);

    }

    /**
     * Call the NIFI rest api to stop the process group
     *
     * @param processGroupFlowEntity
     * @param state
     */
    public void startProcessGroupComponents(ProcessGroupFlowEntity processGroupFlowEntity, String state) {
        processGroupFlow.startOrStopProcessGroupComponents(processGroupFlowEntity, state);

    }


    /**
     * Method to stop all the process group components
     *
     * @param processGroupFlowEntity
     * @return
     */
    public ProcessGroupFlowEntity stopProcessGroupComponents(ProcessGroupFlowEntity processGroupFlowEntity,
                                                              ProcessGroupEntity processorGroup,
                                                              String pgId) {
        stopProcessGroupComponents(processGroupFlowEntity, EntityState.STOPPED.getState());
        checkProcessGroupComponentStatus(processGroupFlowEntity, EntityState.STOPPED.getState(), pgId);
        ProcessGroupFlowEntity pge = processGroupFlow.getLatestProcessGroupFlowEntity(
                processGroupFlowEntity.getProcessGroupFlow().getId());
        return pge;
    }



    public void checkInternalProcessGroupStatus(ProcessGroupFlowEntity processGroupFlowEntity, String state) {
        int count = 0;
        int innerCount = 0;
        ProcessGroupFlowEntity pgfe = null;
        //ProcessGroupFlowEntity currentPgfe = null;


        while (true && count < WAIT_IN_SEC) {
            pgfe = processGroupFlow.getLatestProcessGroupFlowEntity(processGroupFlowEntity.getProcessGroupFlow().getId());

            Set<ProcessGroupEntity> processGroups = pgfe.getProcessGroupFlow().getFlow().getProcessGroups();

            int queuedCount = 0;
            for (ProcessGroupEntity processGroupEntity : processGroups) {

				/*if(! processGroupEntity.getComponent().getContents().getProcessGroups().isEmpty()){
					currentPgfe = getLatestProcessGroupFlowEntity(processGroupEntity.getId());
					checkInternalProcessGroupStatus(currentPgfe, state);
				}*/
                /*
                 * Stop only the necessary process groups for the given process
                 * group ID
                 */
                if (processGroupEntity.getComponent().getParentGroupId()
                        .equalsIgnoreCase(processGroupFlowEntity.getProcessGroupFlow().getId())) {

                    if (state.equalsIgnoreCase(EntityState.STOPPED.getState())) {
                        queuedCount = Integer
                                .parseInt(processGroupEntity.getStatus().getAggregateSnapshot().getQueuedCount().replaceAll(",", ""));
                        // Check for the Runing count
                        if (processGroupEntity.getRunningCount() > 0) {
                            break;
                        }
                        // Check for the queue content
                        if (queuedCount > 0) {
                            flowFileQueue.deleteTheQueueContent(processGroupEntity);
                            break;
                        }

                    }

                    if (state.equalsIgnoreCase(EntityState.RUNNING.getState())
                            && processGroupEntity.getStoppedCount() > 0) {
                        break;
                    }
                }
                innerCount++;
            }

            if (processGroups.size() == innerCount) {
                break;
            }

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {

            }
            count++;
            innerCount = 0;
        }
    }

    /**
     * Check the Process Group Component Status
     *
     * @param processGroupFlowEntity
     * @param state
     */
    public void checkProcessGroupComponentStatus(ProcessGroupFlowEntity processGroupFlowEntity, String state,
                                                 String pgId) {
        checkInternalProcessGroupStatus(processGroupFlowEntity, state);

        ProcessGroupEntity pge = processGroup.getLatestProcessGroupEntity(pgId);

        processGroupFacadeHelper.checkParentProcessGroupStatus(pge, state);
    }


}
