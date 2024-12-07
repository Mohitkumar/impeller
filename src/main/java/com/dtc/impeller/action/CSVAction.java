package com.dtc.impeller.action;

import java.io.StringWriter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Builder;
import org.apache.commons.csv.CSVPrinter;

import com.dtc.impeller.flow.ActionParam;
import com.dtc.impeller.flow.Result;


public class CSVAction extends AbstractAction{

    @ActionParam(optional = true)
    String header ;

    @ActionParam
    String separator;

    @ActionParam
    List<List<Object>> data;

    public CSVAction(String name, int retryCount) {
        super(name, retryCount);
    }


    @Override
    public Result<?, ? extends Throwable> run() {
        StringWriter sw = new StringWriter();
        Builder builder = CSVFormat.newFormat(separator.toCharArray()[0]).builder();
        if(header != null){
            builder.setHeader(header);
        }
        CSVFormat csvFormat = builder
                .setRecordSeparator("\r\n")
                .build();

        try (final CSVPrinter printer = new CSVPrinter(sw, csvFormat)) {
            for (List<Object> columns : data) {
                try{
                   printer.printRecord(columns);
                }catch (Exception e){
                    return Result.error(e);
                }
            }
            return Result.ok(sw.toString().getBytes());
        }catch (Exception e){
            return Result.error(e);
        }
    }
}
