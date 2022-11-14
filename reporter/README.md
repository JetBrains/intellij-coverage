## Report tools
This module contains 4 applications that are used to analyse 
the agent's binary report.

* _Aggregator_ collects results from different modules into a number of
  intermediate binary reports and collects coverage in unloaded classes.
  * Supports binary files in raw hits format generated in case of offline instrumentation
* _Verificator_ sums up coverage statistics and checks if user defined coverage restrictions are passing
* _Reporter_ generates an XML or HTML report from binary reports
  * Supports binary files in raw hits format generated in case of offline instrumentation
