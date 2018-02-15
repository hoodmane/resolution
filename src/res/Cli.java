package res;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Cli {
 private static final Logger log = Logger.getLogger(Cli.class.getName());
 private String[] args = null;
 private Options options = new Options();

 public Cli(String[] args) {
  this.args = args;

  options.addOption("h", "help", false, "show help.");
  options.addOption("p", "prime", true, "The prime");
  options.addOption("t", "tmax", true, "Maximum stem degree");
  options.addOption("m","module", true, "The module file");
  options.addOption("a","Algebra", false, "The algebra");
 }

 public void parse() {
  CommandLineParser parser = new BasicParser();

  CommandLine cmd = null;
  try {
   cmd = parser.parse(options, args);

     if (cmd.hasOption("h"))
       help();
     if(cmd.hasOption("prime"))
	   Config.P = Integer.parseInt(cmd.getOptionValue("prime"));
     Config.Q = 2 * (Config.P - 1);
     if(cmd.hasOption("tmax"))
        Config.T_CAP = Integer.parseInt(cmd.getOptionValue("tmax"));

    } catch (ParseException e) {
       log.log(Level.SEVERE, "Failed to parse comand line properties", e);
       help();
    }
 }

 private void help() {
  // This prints out some help
  HelpFormatter formater = new HelpFormatter();

  formater.printHelp("Main", options);
  System.exit(0);
 }
}

