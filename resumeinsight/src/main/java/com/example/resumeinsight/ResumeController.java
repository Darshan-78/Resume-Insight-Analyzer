package com.example.resumeinsight;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
public class ResumeController {

    @PostMapping("/analyze")
    public ResumeResponse analyze(@RequestBody ResumeRequest request) {

        ResumeAnalyzerService analyzer = new ResumeAnalyzerService();

        ResumeAnalyzerService.Resume resume =
                analyzer.new Resume(request.getText());

               return analyzer.analyze(resume, request.getSelectedRole());
    }
}