package jenkins.plugins.continuum.actions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import hudson.model.InvisibleAction;

/**
 * Invisible action to store in the current run after a Continuum pipeline is initiated,
 * so that downstream steps can use the pipeline ids
 */
public class PipelineInitiatedAction extends InvisibleAction {
    
    private final Map<String, Set<String>> serverPipelineIds;

    public PipelineInitiatedAction(String serverUrl, String pipelineId) {
        this.serverPipelineIds = new HashMap<String, Set<String>>();
        Set<String> pis = new LinkedHashSet<String>();
        pis.add(pipelineId);
        this.serverPipelineIds.put(serverUrl, pis);
    }

    public Set<String> getPipelineIds(String serverUrl) {
        if (!this.serverPipelineIds.containsKey(serverUrl)) {
            return Collections.EMPTY_SET;
        }
        return Collections.unmodifiableSet(this.serverPipelineIds.get(serverUrl));
    }

    public boolean addPipelineId(String serverUrl, String pipelineId) {
        Set<String> pis = null;
        if (!this.serverPipelineIds.containsKey(serverUrl)) {
           pis = new LinkedHashSet<String>();
           this.serverPipelineIds.put(serverUrl, pis);
        }
        else {
            pis = this.serverPipelineIds.get(serverUrl);
        }
        return pis.add(pipelineId);
    }

    public String getLastPipelineId(String serverUrl) {
        Set<String> pis = this.serverPipelineIds.containsKey(serverUrl) ? this.serverPipelineIds.get(serverUrl) : null;
        String pi = null;
        if (pis != null) {
            Iterator<String> piIt = pis.iterator();
            while (piIt.hasNext()) { pi = piIt.next(); }
        }
        return pi;
    }

    public void reset(String serverUrl) {
        Set<String> pis = this.serverPipelineIds.containsKey(serverUrl) ? this.serverPipelineIds.get(serverUrl) : null;
        if (pis != null) {
            pis.clear();
        }
    }
}
