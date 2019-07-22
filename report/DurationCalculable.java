/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.DTNHost;
import java.util.ArrayList;
import java.util.Map;

/**
 * For ContactDurationReport and IntercontactDurationReport purpose 
 * collecting the summary vector for making the Probability Density Function   
 * @author Gregorius Bima, Sanata Dharma University
 */
public interface DurationCalculable {
    
    /**
     * Get the summary vector of Contact Duration 
     * for each node that host has been encountered
     * @return 
     */
    public Map<DTNHost, ArrayList<Double>> getContactDuration();
    
    /**
     * Get the summary vector of Inter-contact Duration 
     * for each node that host has been encountered
     * @return 
     */
    public Map<DTNHost, ArrayList<Double>> getIntercontactDuration();
}
