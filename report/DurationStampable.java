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
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public interface DurationStampable {
    
    public Map<DTNHost, List<Duration>> getEncounterHistory();
    public Map<DTNHost, List<Duration>> getIntercontactHistory();
    
}
