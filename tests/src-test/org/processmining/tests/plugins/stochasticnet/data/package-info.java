/**
 * This package contains files to convert the IBM process collection 2009 (http://www.zurich.ibm.com/csc/bit/downloads.html)
 * Which has been used in these papers:
 * 
 *  Dirk Fahland, Cédric Favre, Barbara Jobstmann, Jana Koehler, Niels Lohmann, Hagen Völzer, and Karsten Wolf:
 *  Instantaneous Soundness Checking of Industrial Business Process Models.
 *  7th Int. Conference on Business Process Management, Springer LNCS 5701, pages 278-293, 2009.
 *
 *  Dirk Fahland, Cédric Favre, Jana Koehler, Niels Lohmann, Hagen Völzer, and Karsten Wolf:
 *  Analysis on Demand: Instantaneous Soundness Checking of Industrial Business Process Models.
 *  Data and Knowledge Engineering 70 (2011) 448-466.
 *  
 * The data can be downloaded and compared to the Petri nets in: 
 * www.service-technology.org/soundness
 * 
 * The difference is that we would like to keep the stochastic branching information in place...
 * 
 * Please note that the tests assume that the XML files are downloaded and unpacked 
 * into the "tests/testfiles/ibm/" folder of this package!
 * 
 * @author Andreas Rogge-Solti
 */

package org.processmining.tests.plugins.stochasticnet.data;

