/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.Duration;
import core.DTNHost;
import java.util.List;
import java.util.Map;

/**
 * For report purpose to collect both contact duration
 * and inter-contact for making Probability Density Function
 * @author Gregorius Bima, Sanata Dharma University
 */
public interface DurationCalculable {
    
    /**
     * Get
     * @return 
     */
    public Map<DTNHost, List<Duration>> getEncounterHistory();
    public Map<DTNHost, List<Duration>> getIntercontactHistory();
    
}
