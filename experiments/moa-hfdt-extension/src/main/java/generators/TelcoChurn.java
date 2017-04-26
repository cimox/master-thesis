package generators;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.*;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Stream generator for Hyperplane data stream.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class TelcoChurn extends AbstractOptionHandler implements InstanceStream {

    @Override
    public String getPurposeString() {
        return "Generates Telco churn prediction dataset from CSV file";
    }

    private static final long serialVersionUID = 1L;


    public IntOption numClassesOption = new IntOption("numClasses", 'c',
            "The number of classes to generate.", 2, 2, Integer.MAX_VALUE);

    public IntOption numNominalsOption = new IntOption("numNominals", 'o',
            "The number of nominal attributes to generate.", 16, 0,
            Integer.MAX_VALUE);

    public IntOption numNumericsOption = new IntOption("numNumerics", 'u',
            "The number of numeric attributes to generate.", 3, 0,
            Integer.MAX_VALUE);

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

    public IntOption numDriftAttsOption = new IntOption("numDriftAtts", 'k',
            "The number of attributes with drift.", 2, 0, Integer.MAX_VALUE);

    public FloatOption magChangeOption = new FloatOption("magChange", 't',
            "Magnitude of the change for every example", 0.0, 0.0, 1.0);

    protected InstancesHeader streamHeader;

    private String csvFile = "data/churn/telco.csv";
    private String csvSplit = ",";
    private BufferedReader csvBufferedReader;
    private HashMap<Long, Attribute> attributeHashMap = new HashMap<>();

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing CSV file...", -1.0);
        generateHeader();
        restart();
    }

    private void generateHeader() {
        JSONParser jsonParser = new JSONParser();
        FastVector attributes = new FastVector();
        long targetClassIndex = 0;

        try {
            JSONArray telcoHeader = (JSONArray) jsonParser.parse(new FileReader("data/churn/telco_header.json"));

            for (Object obj : telcoHeader) {
                if (obj instanceof JSONObject) {
                    JSONObject columnObject = (JSONObject) obj;

                    List<String> attributeValues = new ArrayList<>();
                    String attributeType = null;
                    Long columnPosition = null;

                    Set<String> objectKeys = columnObject.keySet();
                    for (String key : objectKeys) {
                        if (key.equals("values")) {
                            JSONArray values = (JSONArray) columnObject.get(key);

                            for (String value : (Iterable<String>) values) {
                                attributeValues.add(value);
                            }
                        } else if (key.equals("position")) {
                            columnPosition = (Long) columnObject.get(key) - 1;
                        } else if (!key.equals("columnName")) {
                            attributeType = key;
                        }
                    }

                    assert attributeType != null;
                    String column = (String) columnObject.get("columnName");
                    if (attributeType.equals("isNominal")) {
                        if (column.equals("Churn")) {
                            targetClassIndex = columnPosition;
                        }
                        attributes.addElement(new Attribute(column, attributeValues));
                        this.attributeHashMap.put(columnPosition, new Attribute(column, attributeValues));
                    } else if (attributeType.equals("isNumeric")) {
                        attributes.addElement(new Attribute(column));
                        this.attributeHashMap.put(columnPosition, new Attribute(column));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.streamHeader = new InstancesHeader(
                new Instances(getCLICreationString(InstanceStream.class), attributes, 0)
        );
        this.streamHeader.setClassIndex((int) targetClassIndex);
    }

    @Override
    public long estimatedRemainingInstances() {
        return -1;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public boolean hasMoreInstances() {
        return true;
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public InstanceExample nextInstance() {
        double[] attVals = new double[this.numNominalsOption.getValue() + this.numNumericsOption.getValue() + 1];

        try {
            String line = this.csvBufferedReader.readLine();

            if (line == null) {
                restart();
                line = this.csvBufferedReader.readLine();
            }
            String[] lineValues = line.split(this.csvSplit);

            int i = 0;
            for (; i < attVals.length - 1; i++) {
                Attribute attr = this.attributeHashMap.get((long) i);
                String value = lineValues[i + 1];

                if (attr.indexOfValue(value) != -1) {
                    attVals[i] = attr.indexOfValue(value);
                } else {
                    try {
                        attVals[i] = Double.parseDouble(value);
                    } catch (Exception e) {
                        attVals[i] = 0.0;
                    }
                }
            }

            String classValue = lineValues[i + 1];
            Attribute attr = this.attributeHashMap.get((long) i);

            Instance inst = new DenseInstance(1.0, attVals);
            inst.setDataset(getHeader());
            inst.setClassValue(attr.indexOfValue(classValue));
            return new InstanceExample(inst);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void addDrift() {
    }

    @Override
    public void restart() {
        try {
            this.csvBufferedReader = new BufferedReader(new FileReader(this.csvFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        sb.append("Telco churn prediction dataset, src: https://www.ibm.com/communities/analytics/watson-analytics-blog/predictive-insights-in-the-telco-customer-churn-data-set/");
    }
}
