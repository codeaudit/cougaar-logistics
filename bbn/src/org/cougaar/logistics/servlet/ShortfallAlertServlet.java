/*
 *
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */
 
package org.cougaar.logistics.servlet;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.ListAllAgents;

import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Disposition;
import org.cougaar.planning.ldm.plan.Aggregation;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.servlet.data.xml.XMLWriter;

import org.cougaar.logistics.servlet.data.shortfall.ShortfallShortData;
import org.cougaar.logistics.servlet.data.shortfall.FullShortfallData;
import org.cougaar.logistics.servlet.data.shortfall.ShortfallInventoryRule;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.Filters;
import org.cougaar.util.ConfigFinder;

import org.cougaar.logistics.plugin.inventory.ShortfallSummary;
import org.cougaar.logistics.plugin.inventory.ShortfallInventory;

/**
 * A <code>Servlet</code>, loaded by the 
 * <code>SimpleServletComponent</code>, that generates 
 * HTML, XML, and serialized-Object views of Task completion
 * information.
 */
public class ShortfallAlertServlet
extends BaseServletComponent
{
  protected static final UnaryPredicate SHORTFALL_SUMMARY_PRED =
      new UnaryPredicate() {
        public boolean execute(Object o) {
          return (o instanceof ShortfallSummary);
        }
      };

  protected static final String[] iframeBrowsers = {
    "mozilla/5",
    "msie 5",
    "msie 6"
  };

  protected static final String RULE_CONFIG_FILE="ShortfallAlertConfig.txt";

  public static final double DEFAULT_RED_THRESHOLD = 4;

  public static final double DEFAULT_YELLOW_THRESHOLD = 1;

  protected static final int MAX_AGENT_FRAMES = 18;

  protected String path;
    //protected ArrayList rulesList=new ArrayList();

  protected MessageAddress localAgent;

  protected String encLocalAgent;

  protected AgentIdentificationService agentIdService;
  protected BlackboardQueryService blackboardQueryService;
  protected WhitePagesService whitePagesService;

  protected final Object lock = new Object();
  protected LoggingService logger;

  public ShortfallAlertServlet() {
    super();
    path = getDefaultPath();
  }

  public void setParameter(Object o) {
    if (o instanceof String) {
      path = (String) o;
    } else if (o instanceof Collection) {
      Collection c = (Collection) o;
      if (!(c.isEmpty())) {
        path = (String) c.iterator().next();
      }
    } else if (o == null) {
      // ignore
    } else {
      throw new IllegalArgumentException(
          "Invalid parameter: "+o);
    }
  }

  protected String getDefaultPath() {
    return "/shortfall";
  }

  protected String getPath() {
    return path;
  }

  protected Servlet createServlet() {
    return new ShortfallServlet();
  }

  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    if (agentIdService == null) {
      // Revocation
    } else {
      this.localAgent = agentIdService.getMessageAddress();
      encLocalAgent = formURLEncode(localAgent.getAddress());
    }
  }

  public void setBlackboardQueryService(
      BlackboardQueryService blackboardQueryService) {
    this.blackboardQueryService = blackboardQueryService;
  }

  public void setWhitePagesService(
      WhitePagesService whitePagesService) {
    this.whitePagesService = whitePagesService;
  }

  public void load() {
    super.load();
    logger = (LoggingService)
      serviceBroker.getService(this, LoggingService.class, null);

    getAndParseRules();
  }

  public void unload() {
    super.unload();
    if (whitePagesService != null) {
      serviceBroker.releaseService(
          this, WhitePagesService.class, whitePagesService);
      whitePagesService = null;
    }
    if (blackboardQueryService != null) {
      serviceBroker.releaseService(
          this, BlackboardQueryService.class, blackboardQueryService);
      blackboardQueryService = null;
    }
    if (agentIdService != null) {
      serviceBroker.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
  }

  protected void getAndParseRules() {
      File ruleFile = ConfigFinder.getInstance().locateFile(RULE_CONFIG_FILE);
      ArrayList rulesList = new ArrayList();

      BufferedReader ruleStrings=null;

      if(ruleFile == null) {
	  logger.warn("Config file for ShortfallAlertServlet - " + RULE_CONFIG_FILE + " could not be located");
	  return;
      }
      
      try {
	  ruleStrings = new BufferedReader(new FileReader(ruleFile));
      }
      catch(FileNotFoundException ex) {
	  logger.warn("Config file for ShortfallAlertServlet - " + RULE_CONFIG_FILE + " not found");
	  return;
      }

      try {

	String currLine = ruleStrings.readLine();
	while(currLine != null) {
	    if(!(currLine.startsWith("#"))) {
		ShortfallInventoryRule rule = 
		    ShortfallInventoryRule.parseConfigFileLine(currLine);
		if(rule!=null) {
		    rulesList.add(rule);
		}
	    }
	    currLine = ruleStrings.readLine();
	}
	
	ruleStrings.close();

      }
      catch(IOException ex) {
	  throw new RuntimeException("Error while trying to parse config file for ShortfallAlertServlet!",ex); 
      }

      ShortfallShortData.setRulesList(rulesList);
  }
    


  protected List getAllEncodedAgentNames() {
    try {
      // do full WP list (deprecated!)
      Set s = ListAllAgents.listAllAgents(whitePagesService);
      // URLEncode the names and sort
      List l = ListAllAgents.encodeAndSort(s);
      return l;
    } catch (Exception e) {
      throw new RuntimeException(
          "List all agents failed", e);
    }
  }

  protected List getAllAgentNames() {
    try {
      // do full WP list (deprecated!)
      List result = new ArrayList(ListAllAgents.listAllAgents(whitePagesService));
      Collections.sort(result);
      return result;
    } catch (Exception e) {
      throw new RuntimeException(
          "List all agents failed", e);
    }
  }

  protected Collection queryBlackboard(UnaryPredicate pred) {
    return blackboardQueryService.query(pred);
  }

  protected String getEncodedAgentName() {
    return encLocalAgent;
  }

  protected String formURLEncode(String name) {
    try {
      return URLEncoder.encode(name, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      // should never happen
      throw new RuntimeException("Unable to encode to UTF-8?");
    }
  }

  protected String getTitlePrefix() {
    return ""; // must not contain special URL characters
  }

  /**
   * Inner-class that's registered as the servlet.
   */
  protected class ShortfallServlet extends HttpServlet {
    public void doGet(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      (new ShortfallChecker(request, response)).execute();    
    }

    public void doPost(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException, ServletException {
      (new ShortfallChecker(request, response)).execute();  
    }
  }

  /** 
   * Inner-class to hold state and generate the response.
   */
  protected class ShortfallChecker {

    public static final int FORMAT_DATA = 0;
    public static final int FORMAT_XML = 1;
    public static final int FORMAT_HTML = 2;

    private int format;
    private boolean showTables;

    private HttpServletRequest request;
    private HttpServletResponse response;

    // writer from the request for HTML output
    private PrintWriter out;

    // various form params
    double redThreshold;
    double yellowThreshold;
    int refreshInterval;

    public ShortfallChecker(
        HttpServletRequest request, 
        HttpServletResponse response)
    {
      this.request = request;
      this.response = response;
    }         

    public void execute() throws IOException, ServletException 
    {
      try {
        redThreshold = Double.parseDouble(request.getParameter("redThreshold"));
      } catch (Exception e) {
        redThreshold = DEFAULT_RED_THRESHOLD;
      }
      try {
        yellowThreshold = Double.parseDouble(request.getParameter("yellowThreshold"));
      } catch (Exception e) {
        yellowThreshold = DEFAULT_YELLOW_THRESHOLD;
      }
      try {
        refreshInterval = Integer.parseInt(request.getParameter("refreshInterval"));
      } catch (Exception e) {
        refreshInterval = 0;
      }
      String formatParam = request.getParameter("format");
      if (formatParam == null) {
        format = FORMAT_HTML; // default
      } else if ("data".equals(formatParam)) {
        format = FORMAT_DATA;
      } else if ("xml".equals(formatParam)) {
        format = FORMAT_XML;
      } else if ("html".equals(formatParam)) {
        format = FORMAT_HTML;
      } else {
        format = FORMAT_HTML; // other
      }

      String showTablesParam = request.getParameter("showTables");
      if (showTablesParam == null) {
        showTables = false; // default
      } else if ("true".equals(showTablesParam)) {
        showTables = true;
      } else {
        showTables = false; // other
      }

      String viewType = request.getParameter("viewType");
      if (viewType == null) {
        viewDefault(); // default
      } else if ("viewAgentSubmit".equals(viewType)) {
        viewAgentSubmit();
      } else if ("viewAgentBig".equals(viewType)) {
        viewAgentBig();
      } else if ("viewAllAgents".equals(viewType)) {
        viewAllAgents();
      } else if ("viewSelectedAgents".equals(viewType)) {
        viewSelectedAgents();
      } else if ("viewManyAgents".equals(viewType)) {
        viewManyAgents();
      } else if ("viewTitle".equals(viewType)) {
        viewTitle();
      } else if ("viewAgentSmall".equals(viewType)) {
        viewAgentSmall();
      } else if ("viewMoreLink".equals(viewType)) {
        viewMoreLink();
      } else {
        viewDefault(); // other
      }

      // done
    }

    private void viewDefault() throws IOException {
      if (format == FORMAT_HTML) {
        // generate outer frame page:
        //   top:    select "/agent"
        //   middle: "viewAgentSubmit" buttons
        //   bottom: "viewAgentBig" frame
        //
        // Note that the top and middle frames must be on 
        // the same host, due to javascript security.  Only
        // the bottom frame is updated by the submit button.
        response.setContentType("text/html");
        this.out = response.getWriter();
        out.print(
            "<html><head><title>"+
            getTitlePrefix()+
            "Shortfall Viewer</title></head>"+
            "<frameset rows=\"10%,12%,78%\">\n"+
            "<frame src=\""+
            "/agents?format=select&suffix="+
            getEncodedAgentName()+
            "\" name=\"agentFrame\">\n"+
            "<frame src=\"/$"+
            getEncodedAgentName()+getPath()+
            "?viewType=viewAgentSubmit\" name=\"viewAgentSubmit\">\n"+
            "<frame src=\"/$"+
            getEncodedAgentName()+getPath()+
            "?viewType=viewAgentBig\" name=\"viewAgentBig\">\n"+
            "</frameset>\n"+
            "<noframes>Please enable frame support</noframes>"+
            "</html>\n");
        out.flush();
      } else {
        // for other formats, just get the data
        viewAgentBig();
      }
    }

    protected void viewAgentSubmit() throws IOException {
      response.setContentType("text/html");
      this.out = response.getWriter();
      // javascript based on PlanViewServlet
      out.print(
          "<html>\n"+
          "<script language=\"JavaScript\">\n"+
          "<!--\n"+
          "function mySubmit() {\n"+
          "  var obj = top.agentFrame.document.agent.name;\n"+
          "  var encAgent = obj.value;\n"+
          "  if (encAgent.charAt(0) == '.') {\n"+
          "    alert(\"Please select an agent name\")\n"+
          "    return false;\n"+
          "  }\n"+
          "  document.myForm.target=\"viewAgentBig\"\n"+
          "  document.myForm.action=\"/$\"+encAgent+\""+
          getPath()+"\"\n"+
          "  return true\n"+
          "}\n"+
          "// -->\n"+
          "</script>\n"+
          "<head>\n"+
          "<title>"+
          getTitlePrefix()+
          "Shortfall"+
          "</title>"+
          "</head>\n"+
          "<body>"+
          "<form name=\"myForm\" method=\"get\" "+
          "onSubmit=\"return mySubmit()\">\n"+
          getTitlePrefix()+
          "Select an agent above, "+
          "<input type=\"hidden\""+
          " name=\"viewType\""+
          " value=\"viewAgentBig\" "+
          "<input type=\"checkbox\""+
          " name=\"showTables\""+
          " value=\"true\" ");
      if (showTables) {
        out.print("checked");
      }
      out.println(
          "> show table, \n"+
          "<input type=\"submit\""+
          " name=\"formSubmit\""+
          " value=\"Submit\"><br>");
      out.println(
          "<a href=\"/$"+
          getEncodedAgentName()+getPath()+
          "?viewType=viewAllAgents"+
          "\" target=\"_top\">Show all agents.</a>");
      out.println(
          "<a href=\"/$"+
          getEncodedAgentName()+getPath()+
          "?viewType=viewManyAgents"+
          "\" target=\"_top\"> Show several selected agents.</a>");
      out.println("</form>");
      out.print("</body></html>");
      out.flush();
    }

    private void viewAgentBig() {
      // get result
      ShortfallShortData result = getShortfallData();

      // write data
      try {
        if (format == FORMAT_HTML) {
          // html      
          response.setContentType("text/html");
          this.out = response.getWriter();
          printShortfallDataAsHTML(result);
        } else {
          // unsupported
          if (format == FORMAT_DATA) {      
            // serialize
            //response.setContentType("application/binary");
            OutputStream out = response.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(result);
            oos.flush();
          } else {
            // xml
            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();
            out.write(("<?xml version='1.0'?>\n").getBytes());
            XMLWriter w =
              new XMLWriter(
                  new OutputStreamWriter(out));
            result.toXML(w);
            w.flush();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void viewMoreLink() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>"+
          getTitlePrefix()+
          "Shortfall of More Agents</title>\n" +
          "</head>\n"+
          "<body>");
      String firstAgent = request.getParameter("firstAgent");
      if (firstAgent != null) {
        out.println("<A href=\"/$"
                    + getEncodedAgentName()+getPath()
                    + "?viewType=viewAllAgents&refreshInterval="
                    + refreshInterval
                    + "&redThreshold="
                    + redThreshold
                    + "&yellowThreshold="
                    + yellowThreshold
                    + "&firstAgent="
                    + firstAgent
                    + "\" + target=\"_top\">\n"
                    + "<h2><center>More Agents</h2></center>\n"
                    + "</A>");
      }
      out.println("</body>\n</html>");
    }

    private void viewTitle() throws IOException {
      String title = request.getParameter("title");
      response.setContentType("text/html");
      if (refreshInterval > 0) {
        response.setHeader("Refresh", String.valueOf(refreshInterval));
      }
      List agents = getSelectedAgents();
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>" + title + "</title>\n" +
          "</head>\n"+
          "<body>\n"+
          "<h2><center>" + title + "</h2></center>\n");
      int totalAgents = agents.size();
      int nPages = (totalAgents + MAX_AGENT_FRAMES - 1) / MAX_AGENT_FRAMES;
      String[] menu = null;
      if (nPages > 1) {
        menu = new String[nPages]; 
        for (int page = 0; page < nPages; page++) {
          int nagents;
          int agent0;
          agent0 = (page * totalAgents + nPages - 1) / nPages;
          nagents = (((page + 1) * totalAgents + nPages - 1) / nPages) - agent0;
          String item =
            "Agents " + ((String) agents.get(agent0)) +
            " Through " + ((String) agents.get(agent0 + nagents - 1));
          menu[page] = item;
        }
      }
      String thisPage = request.getParameter("thisPage");
      printThresholdAndRefreshForm(out, agents, menu, thisPage);
      out.println(
          "</body>\n"+
          "</html>");
    }

    private void printThresholdAndRefreshForm(PrintWriter out, List selectedAgents, String[] menu, String thisPage) {
      out.println("<form name=\"viewTitle\" action=\"/$" + getEncodedAgentName() + getPath() + "\"method=\"post\" target=\"_top\">");
      out.println("<table>");
      out.print("<tr><td>");
      out.print("Red Threshold");
      out.print("</td><td>");
      out.print("<input name=\"redThreshold\" type=\"text\" value=\""
                + redThreshold
                + "\">");
      out.print("</td><td>");
      out.print("Yellow Threshold");
      out.print("</td><td>");
      out.print("<input name=\"yellowThreshold\" type=\"text\" value=\""
                + yellowThreshold
                + "\">");
      out.println("</td><td rowspan=3>");
      if (menu != null) {
        out.println("<select name=\"page\" size=3 onclick=\"document.viewTitle.submit()\">");
        for (int page = 0; page < menu.length; page++) {
          out.println("<option value=\"" + page + "\" onclick=\"document.viewTitle.submit()\">");
          out.println(menu[page]);
          out.println("</option>");
        }
        out.println("</select>");
      }
      out.println("</td></tr>");
      out.print("<tr><td>");
      out.print("Refresh Interval");
      out.print("</td><td>");
      out.print("<input name=\"refreshInterval\" type=\"text\" value=\""
                + refreshInterval
                + "\">");
      out.print("</td><td>");
      out.print("<input type=\"submit\" name=\"submit\" value=\"Refresh\">");
      out.println("</td>");
      out.println("</tr>");
      out.println("</table>");
      out.println("<input type=\"hidden\" name=\"viewType\" value=\"viewSelectedAgents\">");
      for (int i = 0, n = selectedAgents.size(); i < n; i++) {
        String agentName = (String) selectedAgents.get(i);
        out.println("<input type=\"hidden\" name=\"selectedAgents\" value=\"" + agentName + "\">");
      }
      out.println("<input type=\"hidden\" name=\"currentPage\" value=\"" + thisPage + "\">");
      out.println("</form>");
    }

    // Output a page showing summary info for all agents
    private void viewAllAgents() throws IOException {
      viewSelectedAgents(getAllAgentNames(), "All");
    }

    private void viewSelectedAgents() throws IOException {
      viewSelectedAgents(getSelectedAgents(), "Selected");
    }

    private void viewSelectedAgents(List agents, String titleModifier) throws IOException {
      response.setContentType("text/html");
      if (refreshInterval > 0) {
        response.setHeader("Refresh", String.valueOf(refreshInterval));
      }
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      String title = getTitlePrefix() + "Shortfall of " + titleModifier + " Agents";
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>" + title + "</title>\n" +
          "</head>");
      boolean use_iframes = false;
      String browser = request.getHeader("user-agent").toLowerCase();
      if (browser != null) {
        for (int i = 0; i < iframeBrowsers.length; i++) {
          if (browser.indexOf(iframeBrowsers[i]) >= 0) {
            use_iframes = true;
            break;
          }
        }
      }
      if (use_iframes) {
        out.println(
            "<body>\n"+
            "<h2><center>"+
            title+
            "</h2></center>");
        printThresholdAndRefreshForm(out, agents, null, null);
        for (int i = 0, n = agents.size(); i < n; i++) {
          String agentName = (String) agents.get(i);
          out.println("<iframe src=\"/$"
                      + formURLEncode(agentName)
                      + getPath()
                      + "?viewType=viewAgentSmall&redThreshold="
                      + redThreshold
                      + "&yellowThreshold="
                      + yellowThreshold
                      + "\" scrolling=\"no\" width=300 height=151>"
                      + agentName
                      + "</iframe>");
        }
        out.println("</body>");
      } else {
        int totalAgents = agents.size();
        int nPages = (totalAgents + MAX_AGENT_FRAMES - 1) / MAX_AGENT_FRAMES;
        int nagents;
        int agent0;
        int page;
        if (nPages > 1) {
          try {
            page = Integer.parseInt(request.getParameter("page"));
          } catch (Exception e) {
            try {
              page = Integer.parseInt(request.getParameter("currentPage"));
            } catch (Exception e1) {
              page = 0;
            }
          }
          agent0 = (page * totalAgents + nPages - 1) / nPages;
          nagents = (((page + 1) * totalAgents + nPages - 1) / nPages) - agent0;
          title =
            titleModifier + " Agents " + ((String) agents.get(agent0)) +
            " Through " + ((String) agents.get(agent0 + nagents - 1));
        } else {
          agent0 = 0;
          nagents = totalAgents;
          title = "All Agents";
          page = 0;
        }
        int nrows = (nagents + 2) / 3;
        out.print("<frameset rows=\"100");
        for (int row = 0; row < nrows; row++) {
          out.print(",100");
        }
        out.print("\">\n"
                  + "  <frame src=\"/$"
                  + getEncodedAgentName()
                  + getPath()
                  + "?viewType=viewTitle&title="
                  + getTitlePrefix()+"Shortfall+of+"
                  + formURLEncode(title)
                  + "&refreshInterval="
                  + refreshInterval
                  + "&redThreshold="
                  + redThreshold
                  + "&yellowThreshold="
                  + yellowThreshold
                  + "&thisPage="
                  + page
                  + "&nextPage="
                  + ((page + 1) % nPages));
        for (int i = 0; i < totalAgents; i++) {
          String agentName = (String) agents.get(i);
          out.print("&selectedAgents=" + formURLEncode(agentName));
        }
        out.println("\" scrolling=\"no\">");
        for (int row = 0; row < nrows; row++) {
          out.println("  <frameset cols=\"300,300,300\">");
          for (int col = 0; col < 3; col++) {
            int agentn = agent0 + row * 3 + col;
            if (agentn < agent0 + nagents) {
              String agentName = (String) agents.get(agentn);
              out.println("    <frame src=\""
                          + "/$"
                          + formURLEncode(agentName)
                          + getPath()
                          + "?viewType=viewAgentSmall&redThreshold="
                          + redThreshold
                          + "&yellowThreshold="
                          + yellowThreshold
                          + "\" scrolling=\"no\">");
            } else if (agentn == agent0 + nagents) {
            }
          }
          out.println("  </frameset>");
        }
        out.println("</frameset>");
      }
      out.println("<html>");
    }

    private List getSelectedAgents() {
      String[] selectedAgents = request.getParameterValues("selectedAgents");
      if (selectedAgents != null) {
        List ret = new ArrayList(Arrays.asList(selectedAgents));
        Collections.sort(ret);
        return ret;
      } else {
        return Collections.EMPTY_LIST;
      }
    }

    // Output a checkbox form allowing selection of multiple agents
    private void viewManyAgents() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      SortedSet selectedAgents = new TreeSet(getSelectedAgents());
      boolean selectAll = false;
      boolean selectNone = false;
      String submit = request.getParameter("submit");
      if ("Show".equals(submit)) {
        viewSelectedAgents(new ArrayList(selectedAgents), "Selected");
        return;
      }
      if ("Select All".equals(submit)) {
        selectAll = true;
      } else if ("Select None".equals(submit)) {
        selectNone = true;
      }
      List l = getAllAgentNames();
      Collections.sort(l);
      String title ="Select Agents for Shortfall Display";
      out.println(
          "<html>\n" +
          "<head>\n" +
          "<title>"+
          getTitlePrefix()+
          title+"</title>\n" +
          "</head>");
      out.println(
          "<body>\n"+
          "<h2><center>"+title+"</h2></center>");
      out.println("<form method=\"post\" action=\"/$"+
          getEncodedAgentName()+getPath()+
          "\" target=\"_top\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Select All\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Select None\">");
      out.println("<input type=\"submit\" name=\"submit\" value=\"Show\">");
      out.println("<input type=\"hidden\" name=\"viewType\" value=\"viewManyAgents\">");
      out.println("<table><tr>");
      int nagents = l.size();
      int agent0 = 0;
      int NCOL = 4;
      for (int col = 0; col < NCOL; col++) {
        out.println("<td valign=\"top\">");
        int agent1 = ((col + 1) * nagents + NCOL - 1) / NCOL;
        for (; agent0 < agent1; agent0++) {
          String agentName = (String) l.get(agent0);
          String selected;
          if (selectAll || (!selectNone && selectedAgents.contains(agentName))) {
            selected = " checked=\"true\"";
          } else {
            selected = "";
          }
          out.println("<input type=\"checkbox\" name=\"selectedAgents\" value=\""
                      + agentName
                      + "\""
                      + selected
                      + ">"
                      + agentName
                      + "</input><br>");
        }
        out.println("</td>");
        agent0 = agent1;
      }
      out.println("</tr></table>");
      out.println("</form>");
      out.println("</body>");
      out.println("</html>");
    }


      /***  Moving to shortfall data
    protected int computeNumShortfallWithRules(ShortfallShortData result) {
	Collection summaries = result.getShortfallSummaries().values();
	Iterator summaryIT = summaries.iterator();
	int totalInvsWithShortfall=0;
	while(summaryIT.hasNext()) {
	    ShortfallSummary summary = (ShortfallSummary) summaryIT.next();
	    Iterator invIT = summary.getShortfallInventories().iterator();
	    while(invIT.hasNext()) {
		boolean ruleMatch=false;
		ShortfallInventory shortInv = (ShortfallInventory)invIT.next();
		Iterator rulesIT = rulesList.iterator();
		while(rulesIT.hasNext()) {
		    ShortfallInventoryRule rule = (ShortfallInventoryRule) rulesIT.next();
		    ShortfallInventory newInv = rule.apply(getEncodedAgentName(),shortInv);
		    if(newInv != null) {
			shortInv=newInv;
			ruleMatch=true;
		    }
		}
		if(shortInv.getNumPermShortfall() > 0) {
		    totalInvsWithShortfall++;
		}
	    }

	}
	return totalInvsWithShortfall;
    }
      ***/

    // Output a small page showing summary info for one agent
    private void viewAgentSmall() throws IOException {
      response.setContentType("text/html");
      out = response.getWriter();
      format = FORMAT_HTML;     // Force html format
      String agent = getEncodedAgentName();
      ShortfallShortData result = getShortfallData();
      int numShortfall = result.getNumberOfShortfallInventories();

      int numTempShortfall = result.getNumberOfTempShortfallInventories();

      int numUnexpectedShortfall = result.getNumberOfUnexpectedShortfallInventories();

      String bgcolor, fgcolor, lncolor;
      if ((numUnexpectedShortfall - numTempShortfall) >= redThreshold) {
        bgcolor = "#aa0000";
        fgcolor = "#ffffff";
        lncolor = "#ffff00";
      } else if ((numUnexpectedShortfall - numTempShortfall >= yellowThreshold) || (numTempShortfall >= yellowThreshold)) {
        bgcolor = "#ffff00";
        fgcolor = "#000000";
        lncolor = "#0000ff";
      } else {
        bgcolor = "#d0ffd0";
        fgcolor = "#000000";
        lncolor = "#0000ff";
      }
      out.println(
		  "<html>\n"+
		  "<head>\n"+
		  "</head>\n"+
		  "<body"+
		  " bgcolor=\"" + bgcolor + 
		  "\" text=\"" + fgcolor + 
		  "\" vlink=\"" + lncolor + 
		  "\" link=\"" + lncolor + "\">\n"+
		  "<pre><a href=\""+
		  "/$" + agent + getPath() +
		  "\" target=\"_top\">"+
		  agent+
		  "</a>");
      out.println(formatLabel("Num Shortfall Inventories:") + " <b>" + numShortfall + "</b>");
      out.println(formatLabel("Num Temp Shortfall Inventories:") + " <b>" + numTempShortfall + "</b>");
      out.println(formatLabel("Num Unexpected Shortfall Inventories:") + " <b>" + numUnexpectedShortfall + "</b>");
      out.println(formatLabel("Effected Supply Types:\n") + result.getSupplyTypes());
      out.println("</body>\n</html>");
    }

    private String formatLabel(String lbl) {
      int nchars = lbl.length();
      if (nchars > 24) return lbl;
      return lbl + "                        ".substring(nchars);
    }

    private String formatInteger(int n) {
      return formatInteger(n, 5);
    }

    private final String SPACES = "                    ";
    private final int NSPACES = SPACES.length();

    private String formatInteger(int n, int w) {
      if (w > NSPACES) w = NSPACES;
      String r = String.valueOf(n);
      int needed = w - r.length();
      if (needed <= 0) return r;
      return SPACES.substring(0, needed) + r;
    }

    private String formatPercent(double percent) {
      return formatInteger((int) (percent * 100.0), 3) + "%";
    }

    private String formatColorBar(String color) {
      return 
        "<table width=\"100%\" bgcolor=\""+color+
        "\"><tr><td>&nbsp;</td></tr></table>";
    }

    protected Collection getAllShortfallSummaries() {
      Collection col = queryBlackboard(SHORTFALL_SUMMARY_PRED);
      if (col == null) col = Collections.EMPTY_LIST;
      return col;
    }



    public Collection filterTasks(Collection allTasks, UnaryPredicate predicate) {
      return Filters.filter(allTasks, predicate);
    }

    protected ShortfallShortData getShortfallData() {
      // get tasks
      Collection summaries = getAllShortfallSummaries();
      long nowTime = System.currentTimeMillis();
      ShortfallShortData data;
      if(showTables) {
	  data = new FullShortfallData(getEncodedAgentName(),nowTime,summaries);
      }
      else {
	  data = new ShortfallShortData(getEncodedAgentName(),nowTime,summaries);
      }
      return data;
    }



 


    /**
     * Get the TASKS.PSP URL for the given UID String.
     *
     * Assumes that the TASKS.PSP URL is fixed at "/tasks".
     */
    protected String getTaskUID_URL(
        String clusterId, String sTaskUID) {
      /*
        // FIXME prefix with base URL?
        
        String baseURL =   
          request.getScheme()+
          "://"+
          request.getServerName()+
          ":"+
          request.getServerPort()+
          "/";
      */
      return 
        "/$"+
        clusterId+
        "/tasks?mode=3&uid="+
        sTaskUID;
    }

    /**
     * Get a brief description of the given <code>PlanElement</code>.
     */
    protected String getPlanElement(
        PlanElement pe) {
      return 
        (pe instanceof Allocation) ?
        "Allocation" :
        (pe instanceof Expansion) ?
        "Expansion" :
        (pe instanceof Aggregation) ?
        "Aggregation" :
        (pe instanceof Disposition) ?
        "Disposition" :
        (pe instanceof AssetTransfer) ?
        "AssetTransfer" :
        (pe != null) ?
        pe.getClass().getName() : 
        null;
    }

    /**
     * Write the given <code>ShortfallShortData</code> as formatted HTML.
     */
    protected void printShortfallDataAsHTML(ShortfallShortData result) {
      // javascript based on PlanViewServlet
      out.print(
          "<html><body>\n"+
          "<h2><center>"+
          getTitlePrefix()+
          "Shortfall at "+
          getEncodedAgentName()+
          "</center></h2>\n");
      printCountersAsHTML(result);
      printTablesAsHTML(result);
      out.print("</body></html>");
      out.flush();
    }


    protected void printCountersAsHTML(ShortfallShortData result) {
      int numShortfall = result.getNumberOfShortfallInventories();
      int numTempShortfall = result.getNumberOfTempShortfallInventories();
      int numUnexpectedShortfall = result.getNumberOfUnexpectedShortfallInventories();
      String shortfallColor;
      if ((numUnexpectedShortfall - numTempShortfall) >= redThreshold) {
        shortfallColor = "red";
      } else if (((numUnexpectedShortfall - numTempShortfall) >= yellowThreshold) || (numTempShortfall >= yellowThreshold)) {
        shortfallColor = "yellow";
      } else {
        shortfallColor = "#00d000";
      }
      out.print(
          formatColorBar(shortfallColor)+
          "<pre>\n"+
          "Time: <b>");
      long timeMillis = result.getTimeMillis();
      out.print(new Date(timeMillis));
      out.print("</b>   (");
      out.print(timeMillis);
      out.print(" MS)\n"+
          getTitlePrefix()+
          "Number of Shortfall Inventories: <b>"+
          numShortfall +
          "</b>\n");
      out.print("Number of Temporary Shortfall Inventories: <b>"+
          numTempShortfall +
          "</b>\n");
      out.print("Number of Unexpected Shortfall Inventories: <b>"+
          numUnexpectedShortfall +
          "</b>\n");
      out.println(formatLabel("Effected Supply Types:") + ((result.getSupplyTypes()).replaceAll("\n","")) + "\n");
      out.print("</pre>\n");
    }

    protected void printTablesAsHTML(ShortfallShortData result) {
      if (result instanceof FullShortfallData) {
	Iterator summaries = result.getShortfallSummaries().values().iterator();
	while(summaries.hasNext()) {
	    ShortfallSummary summary = (ShortfallSummary) summaries.next();
	    String supplyType = summary.getSupplyType();
	    int numShortfallInvs = summary.getShortfallInventories().size();
	    
	    beginShortfallHTMLTable(
			       (supplyType + " shortfall items:"+numShortfallInvs+"]"),
			       "wFailedTasks");
	    printShortfallSummaryAsHTML(summary);

	    endShortfallHTMLTable();
	}
      } else {
        // no table data
        out.print(
            "<p>"+
            "<a href=\"");
        out.print("/$");
        out.print(getEncodedAgentName());
        out.print(getPath());
        out.print(
            "?showTables=true&viewType=viewAgentBig\" target=\"viewAgentBig\">"+
            "Full Listing of Shortfall Inventory Items (");
        out.print(
            (result.getNumberOfShortfallInventories()));
        out.println(
            " lines)</a><br>");
      }
    }
    /**
     * Begin a table of <tt>printShortfallSummaryAsHTML</tt> entries.
     */
    protected void beginShortfallHTMLTable(
        String title, String subTitle) {
      out.print(
          "<table border=1 cellpadding=3 cellspacing=1 width=\"100%\">\n"+
          "<tr bgcolor=lightgrey><th align=left colspan=7>");
      out.print(title);
      if (subTitle != null) {
        out.print(
            "&nbsp;&nbsp;<tt><i>");
        out.print(subTitle);
        out.print("</i></tt>");
      }
      out.print(
          "</th></tr>\n"+
          "<tr>"+
          "<th></th>"+
          "<th>Inventory Item</th>"+
          "<th>Demand Shortfall</th>"+
          "<th>Refill Shortfall</th>"+
          "<th>Supply Shortfall</th>"+
	  "<th>Temp Shortfall</th>"+
          "<th>ProjectSupply Shortfall</th>"+
          "</tr>\n");
    }

    /**
     * End a table of <tt>printShortfallSummaryAsHTML</tt> entries.
     */
    protected void endShortfallHTMLTable() {
      out.print(
          "</table>\n"+
          "<p>\n");
    }

    /**
     * Write the given <code>AbstractTask</code> as formatted HTML.
     */
    protected void printShortfallSummaryAsHTML(ShortfallSummary summary) {
      Iterator inventoryItems = summary.getShortfallInventories().iterator();
      int index=0;
      while(inventoryItems.hasNext()) {
	  ShortfallInventory inv = (ShortfallInventory) inventoryItems.next();
	  String invItem = inv.getInvID();
	  out.print("<tr align=left><td>");
	  out.print(++index);
	  out.print("</td><td>");
	  out.print("<b>" + invItem + "</b>");
	  out.print("</td><td>");
	  out.print("<b>" + inv.getNumDemand() + "</b>");
	  out.print("</td><td>");
	  out.print("<b>" + inv.getNumRefill() + "</b>");
	  out.print("</td><td>");
	  out.print("<b>" + inv.getNumActual() + "</b>");
	  out.print("</td><td>");
	  out.print("<b>" + inv.getNumTempShortfall() + "</b>");
	  out.print("</td><td>");
	  out.print("<b>" + inv.getNumProjection() + "</b>");
	  out.print("</td></tr>\n");
      }
    }
  }
}
