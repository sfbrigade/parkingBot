package org.sfgov.police.autotrial;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {
  private static Logger LOG = LoggerFactory.getLogger(MainController.class);

  @RequestMapping(path="/", method=RequestMethod.GET)
  public ResponseEntity<byte[]> root(@RequestParam(value="name") String name, @RequestParam(value="address") String address, @RequestParam(value="citationId") String citation) {
    InputStream is = this.getClass().getResourceAsStream("letter.tex");
    BufferedInputStream bis = new BufferedInputStream(is);
    byte[] tex = null;
    try {
      tex = new byte[bis.available()+1];
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    try {
      bis.read(tex);
    } catch(IOException e) {
      LOG.error(e.getMessage(), e);
    }

    String texSource = null;
    try { 
      texSource = new String(tex, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage(), e);
    }
    LOG.debug(texSource + " is the contents of /letter.tex");

    // TODO add format validation for parameters

    String lAtEx = texSource.replace("${citation}", citation).replace("${address}", address).replace("${name}", name);
    LOG.debug(lAtEx + "<== filled in LaTeX, now write to a temporary file and invoke xelatex on it");
    
    File finalTeXSource = null;
    try {
      finalTeXSource = File.createTempFile("latter", ".tex");
      FileWriter texWriter_ = new FileWriter(finalTeXSource);
      BufferedWriter texWriter = new BufferedWriter(texWriter_);
      texWriter.write(lAtEx);
      texWriter.flush();
      LOG.debug("wrote LaTeX to "+finalTeXSource.getCanonicalPath());
    } catch (Throwable t) {
      LOG.error(t.getMessage(), t);
    }

    try {
      Process p= new ProcessBuilder("/usr/local/bin/latexmk", "-xelatex", "-pdf", finalTeXSource.getAbsolutePath()).start();
      p.waitFor();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    
    String finalName = null;
    try { 
      finalName = finalTeXSource.getCanonicalPath().replace(".tex", ".pdf");
    } catch(IOException e) {
      LOG.error(e.getMessage(), e);
    }
    BufferedInputStream pdfIs = null;
    try {
      pdfIs = new BufferedInputStream(new FileInputStream(finalName));
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    byte[] pdf = new byte[new Long(new File(finalName).length()).intValue()];
    try {
      pdfIs.read(pdf);
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
    }
    return new ResponseEntity<byte[]>(pdf,HttpStatus.OK); 
  }
}
