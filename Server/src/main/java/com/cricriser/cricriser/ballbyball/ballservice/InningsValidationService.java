package com.cricriser.cricriser.ballbyball.ballservice;

import org.springframework.stereotype.Service;

@Service
public class InningsValidationService {

    public boolean isInningsOver(
            int wickets,
            double overs,
            int maxOvers,
            Integer target,
            int currentRuns
    ) {

        if (wickets >= 10) return true;

        if (overs >= maxOvers) return true;

        if (target != null && currentRuns >= target) return true;

        return false;
    }

}
