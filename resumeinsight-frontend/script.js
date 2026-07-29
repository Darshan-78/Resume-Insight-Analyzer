// Cookie helper function
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Session initialization
let sessionId = getCookie("resume_session_id");
if (!sessionId) {
    sessionId = typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : 'session_' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
    document.cookie = "resume_session_id=" + sessionId + "; max-age=" + (30 * 24 * 60 * 60) + "; path=/; SameSite=Lax";
}

// Determine backend API base URL dynamically to support both local development and production
const API_BASE = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1' || !window.location.hostname)
    ? (window.location.port === '8080' ? '' : 'http://localhost:8080')
    : 'https://resumeinsight-fz38.onrender.com';

// Global Application State Variables
let currentAnalysisId = null;
let uploadedFilename = null;
let uploadedFileText = null; // Decoupled text state for uploaded file
let scoreChart = null;
let lastInteractedInput = "file"; // Track last input interaction (file or text)
let uploadedAtsWarnings = []; // ATS warnings from file upload

// DOM Elements
const resumeText = document.getElementById("resumeText");
const jobDescriptionText = document.getElementById("jobDescriptionText");
const charCount = document.getElementById("charCount");
const analysisMode = document.getElementById("analysisMode");
const roleDropdownGroup = document.getElementById("roleDropdownGroup");
const roleDropdown = document.getElementById("roleDropdown");
const jdGroup = document.getElementById("jdGroup");
const dropZone = document.getElementById("dropZone");
const fileInput = document.getElementById("fileInput");
const fileStatus = document.getElementById("fileStatus");

const loader = document.getElementById("loader");
const resultPlaceholder = document.getElementById("resultPlaceholder");
const resultsDashboard = document.getElementById("resultsDashboard");

// Toggle Input Sections based on Mode Selection
analysisMode.addEventListener("change", () => {
    if (analysisMode.value === "jd") {
        jdGroup.classList.remove("hidden");
        roleDropdownGroup.classList.add("hidden");
    } else {
        jdGroup.classList.add("hidden");
        roleDropdownGroup.classList.remove("hidden");
    }
});

// Update character counter for Resume text
resumeText.addEventListener("input", () => {
    lastInteractedInput = "text";
    updateCharacterCount();
    const validationMessage = document.getElementById("validationMessage");
    if (validationMessage && resumeText.value.trim()) {
        validationMessage.classList.add("hidden");
        validationMessage.textContent = "";
    }
});

resumeText.addEventListener("focus", () => {
    lastInteractedInput = "text";
});

jobDescriptionText.addEventListener("input", () => {
    const validationMessage = document.getElementById("validationMessage");
    if (validationMessage && jobDescriptionText.value.trim()) {
        validationMessage.classList.add("hidden");
        validationMessage.textContent = "";
    }
});

function updateCharacterCount() {
    const count = resumeText.value.length;
    charCount.textContent = `${count} characters`;
    if (count > 0 && count < 300) {
        charCount.style.color = "#E8A33D"; // Amber warning for short resumes
    } else if (count > 2500) {
        charCount.style.color = "#E8A33D"; // Amber warning for long resumes
    } else {
        charCount.style.color = "#718096";
    }
}

// Drag & Drop File Upload Handlers
dropZone.addEventListener("click", () => {
    lastInteractedInput = "file";
    fileInput.click();
});

// Fallback mobile click handler
const mobileUploadBtn = document.getElementById("mobileUploadBtn");
if (mobileUploadBtn) {
    mobileUploadBtn.addEventListener("click", (e) => {
        e.stopPropagation(); // Prevent bubbling up to dropZone click event
        lastInteractedInput = "file";
        fileInput.click();
    });
}

dropZone.addEventListener("dragover", (e) => {
    e.preventDefault();
    dropZone.classList.add("dragover");
});

dropZone.addEventListener("dragleave", () => {
    dropZone.classList.remove("dragover");
});

dropZone.addEventListener("drop", (e) => {
    e.preventDefault();
    dropZone.classList.remove("dragover");
    if (e.dataTransfer.files.length > 0) {
        lastInteractedInput = "file";
        handleFileUpload(e.dataTransfer.files[0]);
    }
});

fileInput.addEventListener("change", (e) => {
    if (e.target.files.length > 0) {
        lastInteractedInput = "file";
        handleFileUpload(e.target.files[0]);
    }
});

function handleFileUpload(file) {
    const ext = file.name.substring(file.name.lastIndexOf(".")).toLowerCase();
    if (ext !== ".pdf" && ext !== ".docx" && ext !== ".doc") {
        alert("Only PDF, DOCX, and DOC files are supported.");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    fileStatus.textContent = "Extracting text from file...";

    fetch(`${API_BASE}/upload`, {
        method: "POST",
        headers: {
            "X-Session-Id": sessionId
        },
        body: formData
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("HTTP error " + response.status);
        }
        return response.json();
    })
    .then(data => {
        if (data.error) {
            alert(data.error);
            fileStatus.textContent = "";
        } else {
            uploadedFileText = data.text;
            uploadedFilename = data.filename;
            fileStatus.textContent = `File Uploaded: ${data.filename}`;
            lastInteractedInput = "file";
            uploadedAtsWarnings = data.atsWarnings || [];
            const validationMessage = document.getElementById("validationMessage");
            if (validationMessage) {
                validationMessage.classList.add("hidden");
                validationMessage.textContent = "";
            }
        }
    })
    .catch(err => {
        console.error(err);
        alert("Failed to parse PDF resume: " + err.message);
        fileStatus.textContent = "";
    });
}

// Clear all inputs
function clearInputs() {
    resumeText.value = "";
    jobDescriptionText.value = "";
    fileInput.value = "";
    fileStatus.textContent = "";
    uploadedFilename = null;
    uploadedFileText = null;
    currentAnalysisId = null;
    lastInteractedInput = "file";
    uploadedAtsWarnings = [];
    updateCharacterCount();
    
    // Reset collapsible UI
    const content = document.getElementById("pasteSectionContent");
    const arrow = document.getElementById("toggleArrow");
    content.classList.add("hidden");
    arrow.classList.remove("rotated");
    
    // Reset display
    document.getElementById("healthCheckCard").classList.add("hidden");
    resultPlaceholder.classList.remove("hidden");
    resultsDashboard.classList.add("hidden");
    loader.classList.add("hidden");

    // Hide validation message
    const validationMessage = document.getElementById("validationMessage");
    if (validationMessage) {
        validationMessage.classList.add("hidden");
        validationMessage.textContent = "";
    }

    // Hide entire results section
    const resultsSectionWrapper = document.getElementById("resultsSectionWrapper");
    if (resultsSectionWrapper) {
        resultsSectionWrapper.classList.add("hidden");
    }

    // Show AI hero section
    const aiHeroSection = document.getElementById("aiHeroSection");
    if (aiHeroSection) {
        aiHeroSection.classList.remove("hidden");
    }
}

// Expand / Collapse Paste Text input area
function togglePasteSection() {
    const content = document.getElementById("pasteSectionContent");
    const arrow = document.getElementById("toggleArrow");
    if (content.classList.contains("hidden")) {
        content.classList.remove("hidden");
        arrow.classList.add("rotated");
    } else {
        content.classList.add("hidden");
        arrow.classList.remove("rotated");
    }
}

// Perform Resume Analysis
function analyzeResume() {
    let textVal = "";
    let filenameVal = "Pasted Resume Text";

    // Prioritize by user's most recent interaction, falling back to file if text is blank
    if (lastInteractedInput === "file" && uploadedFileText) {
        textVal = uploadedFileText.trim();
        filenameVal = uploadedFilename;
    } else if (resumeText.value.trim()) {
        textVal = resumeText.value.trim();
        filenameVal = "Pasted Resume Text";
    } else if (uploadedFileText) {
        textVal = uploadedFileText.trim();
        filenameVal = uploadedFilename;
    }

    const validationMessage = document.getElementById("validationMessage");
    if (validationMessage) {
        validationMessage.classList.add("hidden");
        validationMessage.textContent = "";
    }

    if (!textVal) {
        if (validationMessage) {
            validationMessage.textContent = "Please upload a resume or paste text";
            validationMessage.classList.remove("hidden");
            validationMessage.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        } else {
            alert("Please upload a resume or paste text");
        }
        return;
    }

    // Determine target variables
    const mode = analysisMode.value;
    const jdVal = mode === "jd" ? jobDescriptionText.value.trim() : null;
    const selectedRole = mode === "role" ? roleDropdown.value : null;

    if (mode === "jd" && !jdVal) {
        if (validationMessage) {
            validationMessage.textContent = "Please paste the target Job Description to match against";
            validationMessage.classList.remove("hidden");
            validationMessage.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        } else {
            alert("Please paste the target Job Description to match against");
        }
        return;
    }

    // Show Results Section Wrapper and Loader
    const resultsSectionWrapper = document.getElementById("resultsSectionWrapper");
    if (resultsSectionWrapper) {
        resultsSectionWrapper.classList.remove("hidden");
    }

    // Hide AI hero section
    const aiHeroSection = document.getElementById("aiHeroSection");
    if (aiHeroSection) {
        aiHeroSection.classList.add("hidden");
    }

    loader.classList.remove("hidden");
    resultPlaceholder.classList.add("hidden");
    resultsDashboard.classList.add("hidden");

    const payload = {
        text: textVal,
        selectedRole: selectedRole,
        jobDescription: jdVal,
        filename: filenameVal,
        atsWarnings: lastInteractedInput === "file" ? uploadedAtsWarnings : []
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
        controller.abort();
    }, 30000); // 30 seconds timeout fallback

    fetch(`${API_BASE}/analyze`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-Session-Id": sessionId
        },
        body: JSON.stringify(payload),
        signal: controller.signal
    })
    .then(response => {
        clearTimeout(timeoutId);
        if (!response.ok) {
            throw new Error("Failed to connect to backend server.");
        }
        return response.json();
    })
    .then(data => {
        loader.classList.add("hidden");
        renderResults(data);
    })
    .catch(err => {
        clearTimeout(timeoutId);
        loader.classList.add("hidden");
        resultPlaceholder.classList.remove("hidden");
        
        if (err.name === 'AbortError') {
            alert("Analysis request timed out after 30 seconds. Please check your backend server and try again.");
        } else {
            alert("Error executing analysis: " + err.message);
        }
    });
}

// Render Results Dashboard
function renderResults(data) {
    currentAnalysisId = data.id;
    resultsDashboard.classList.remove("hidden");

    // Score display number
    const scoreVal = data.score || 0;
    document.getElementById("scoreNumber").textContent = `${scoreVal}%`;
    document.getElementById("recommendationText").textContent = data.bestRoleRecommendation;

    // Overview metrics
    const techMatched = data.matchedSkills ? data.matchedSkills.length : 0;
    const softMatched = data.matchedSoftSkills ? data.matchedSoftSkills.length : 0;
    const techMissing = data.missingSkills ? data.missingSkills.length : 0;
    const softMissing = data.missingSoftSkills ? data.missingSoftSkills.length : 0;

    document.getElementById("overviewScore").textContent = `${scoreVal}%`;
    document.getElementById("overviewRole").textContent = data.bestRoleRecommendation || "-";
    document.getElementById("overviewMatchedCount").textContent = `${techMatched} Tech, ${softMatched} Soft`;
    document.getElementById("overviewMissingCount").textContent = `${techMissing} Tech, ${softMissing} Soft`;

    // Update circular progress ring
    const fill = document.getElementById("scoreRingFill");
    if (fill) {
        const offset = 364.42 - (364.42 * scoreVal / 100);
        fill.style.strokeDashoffset = offset;
        
        let color = "#E8A33D"; // Warm Amber/Orange for low scores
        if (scoreVal >= 70) {
            color = "url(#scoreGrad)"; // gradient combination
        } else if (scoreVal >= 40) {
            color = "#F59E0B"; // Amber
        }
        fill.style.stroke = color;
        const finalColor = scoreVal >= 70 ? "#10B981" : (scoreVal >= 40 ? "#F59E0B" : "#E8A33D");
        document.getElementById("scoreNumber").style.color = finalColor;
        document.getElementById("overviewScore").style.color = finalColor;
    }

    // Reset collapsible grammar state
    const toggleGrammarBtn = document.getElementById("toggleGrammarBtn");
    const grammarContent = document.getElementById("grammarContent");
    if (grammarContent) {
        grammarContent.classList.add("hidden");
    }
    if (toggleGrammarBtn) {
        if (data.grammarIssues && data.grammarIssues.length > 0) {
            toggleGrammarBtn.classList.remove("hidden");
            toggleGrammarBtn.textContent = "Show details";
        } else {
            toggleGrammarBtn.classList.add("hidden");
        }
    }

    // Reset collapsible roadmap state
    const toggleRoadmapBtn = document.getElementById("toggleRoadmapBtn");
    const roadmapCollapsible = document.getElementById("roadmapCollapsible");
    if (toggleRoadmapBtn) {
        toggleRoadmapBtn.classList.add("hidden");
    }
    if (roadmapCollapsible) {
        roadmapCollapsible.classList.add("hidden");
    }

    // Populate Matched Skills
    const matchedList = document.getElementById("matchedSkillsList");
    matchedList.innerHTML = "";
    if (data.matchedSkills && data.matchedSkills.length > 0) {
        data.matchedSkills.forEach(skill => {
            const tag = document.createElement("span");
            tag.className = "skill-tag skill-tag-matched";
            tag.textContent = skill;
            matchedList.appendChild(tag);
        });
    } else {
        matchedList.innerHTML = "<p class='placeholder-text' style='margin:0;font-size:12px;'>No matching tech skills found.</p>";
    }

    // Populate Missing Skills (with priority labels)
    const missingList = document.getElementById("missingSkillsList");
    missingList.innerHTML = "";
    let techMissingPrioritized = [];
    if (data.missingSkillsPrioritized && data.missingSkillsPrioritized.length > 0) {
        techMissingPrioritized = data.missingSkillsPrioritized.filter(item => {
            return data.missingSkills && data.missingSkills.includes(item.name);
        });
    }

    if (techMissingPrioritized.length > 0) {
        techMissingPrioritized.forEach(item => {
            const tag = document.createElement("span");
            tag.className = "skill-tag skill-tag-missing";
            const priorityClass = item.priority === "Critical" ? "critical" : "nice";
            const priorityText = item.priority === "Critical" ? "Critical" : "Nice to have";
            tag.innerHTML = `${item.name} <span class="priority-lbl ${priorityClass}">${priorityText}</span>`;
            missingList.appendChild(tag);
        });
    } else if (data.missingSkills && data.missingSkills.length > 0) {
        data.missingSkills.forEach(skill => {
            const tag = document.createElement("span");
            tag.className = "skill-tag skill-tag-missing";
            tag.textContent = skill;
            missingList.appendChild(tag);
        });
    } else {
        missingList.innerHTML = "<p class='placeholder-text' style='margin:0;font-size:12px;'>No missing tech skills!</p>";
    }

    // Populate Matched Soft Skills
    const matchedSoftList = document.getElementById("matchedSoftSkillsList");
    matchedSoftList.innerHTML = "";
    if (data.matchedSoftSkills && data.matchedSoftSkills.length > 0) {
        data.matchedSoftSkills.forEach(skill => {
            const tag = document.createElement("span");
            tag.className = "skill-tag skill-tag-matched";
            tag.textContent = skill;
            matchedSoftList.appendChild(tag);
        });
    } else {
        matchedSoftList.innerHTML = "<p class='placeholder-text' style='margin:0;font-size:12px;'>No matching soft skills found.</p>";
    }

    // Populate Missing Soft Skills
    const missingSoftList = document.getElementById("missingSoftSkillsList");
    missingSoftList.innerHTML = "";
    let softMissingPrioritized = [];
    if (data.missingSkillsPrioritized && data.missingSkillsPrioritized.length > 0) {
        softMissingPrioritized = data.missingSkillsPrioritized.filter(item => {
            return data.missingSoftSkills && data.missingSoftSkills.includes(item.name);
        });
    }

    if (softMissingPrioritized.length > 0) {
        softMissingPrioritized.forEach(item => {
            const tag = document.createElement("span");
            tag.className = "skill-tag skill-tag-missing";
            const priorityClass = item.priority === "Critical" ? "critical" : "nice";
            const priorityText = item.priority === "Critical" ? "Critical" : "Nice to have";
            tag.innerHTML = `${item.name} <span class="priority-lbl ${priorityClass}">${priorityText}</span>`;
            missingSoftList.appendChild(tag);
        });
    } else if (data.missingSoftSkills && data.missingSoftSkills.length > 0) {
        data.missingSoftSkills.forEach(skill => {
            const tag = document.createElement("span");
            tag.className = "skill-tag skill-tag-missing";
            tag.textContent = skill;
            missingSoftList.appendChild(tag);
        });
    } else {
        missingSoftList.innerHTML = "<p class='placeholder-text' style='margin:0;font-size:12px;'>No missing soft skills!</p>";
    }

    // Toggle custom soft-skills suggestion tip
    const softTip = document.getElementById("softSkillsSuggestion");
    if (data.missingSoftSkills && data.missingSoftSkills.length > 0) {
        softTip.classList.remove("hidden");
    } else {
        softTip.classList.add("hidden");
    }

    // Show Role leaderboards (If in Role Match mode and Rankings exist)
    const leaderboardCard = document.getElementById("rolesLeaderboardCard");
    const leaderboardList = document.getElementById("leaderboardList");
    leaderboardList.innerHTML = "";

    if (!data.jdMatchMode && data.roleRanking && data.roleRanking.length > 0) {
        leaderboardCard.classList.remove("hidden");
        
        // Find best score for conditional bar coloring
        const bestScore = data.roleRanking[0].score;

        data.roleRanking.forEach(rank => {
            const row = document.createElement("div");
            row.className = "leaderboard-row";

            // Coloring bars conditionally based on scoring index
            let fillClass = "active-mid";
            if (rank.score === bestScore && rank.score >= 50) {
                fillClass = "active-best";
            }

            row.innerHTML = `
                <div class="leaderboard-info">
                    <span>${rank.role}</span>
                    <span>${rank.score}%</span>
                </div>
                <div class="leaderboard-bar-bg">
                    <div class="leaderboard-bar-fill ${fillClass}" style="width: ${rank.score}%"></div>
                </div>
            `;
            leaderboardList.appendChild(row);
        });
    } else {
        leaderboardCard.classList.add("hidden");
    }

    // Populate Resume Health Check
    const healthCheckCard = document.getElementById("healthCheckCard");
    healthCheckCard.classList.remove("hidden");

    // Local computed variables for diagnostics fallbacks (e.g. for historical reloads)
    const structureSuggestions = data.structureSuggestions || checkResumeStructureClient(data.text);
    const bulletFeedback = data.bulletFeedback || checkBulletPointsClient(data.text);
    const atsWarnings = data.atsWarnings || ["ATS check not applicable (historical record)"];

    // 1. Structure Checker
    const structureList = document.getElementById("structureList");
    structureList.innerHTML = "";
    if (structureSuggestions && structureSuggestions.length > 0) {
        structureSuggestions.forEach(sug => {
            const li = document.createElement("li");
            li.textContent = sug;
            structureList.appendChild(li);
        });
    } else {
        structureList.innerHTML = "<li class='success-item'>All standard expected sections (Contact Info, Education, Skills, Projects/Experience) detected!</li>";
    }

    // 2. Bullet Point Strength Checker
    const bulletFeedbackList = document.getElementById("bulletFeedbackList");
    bulletFeedbackList.innerHTML = "";
    if (bulletFeedback && bulletFeedback.length > 0) {
        bulletFeedback.forEach(item => {
            const li = document.createElement("li");
            const originalDiv = document.createElement("div");
            originalDiv.className = "bullet-original";
            originalDiv.textContent = item.originalBullet;
            
            const suggDiv = document.createElement("div");
            suggDiv.className = "bullet-suggestion";
            suggDiv.innerHTML = item.suggestions.join("<br>");

            li.appendChild(originalDiv);
            li.appendChild(suggDiv);
            bulletFeedbackList.appendChild(li);
        });
    } else {
        bulletFeedbackList.innerHTML = "<li class='success-item'>All bullet points have strong action verbs and quantifiable metrics!</li>";
    }

    // 3. ATS Friendliness Check
    const atsStatus = document.getElementById("atsStatus");
    atsStatus.innerHTML = "";
    if (atsWarnings && atsWarnings.length > 0) {
        const wordOrPasted = atsWarnings.includes("N/A for Word documents") || atsWarnings.includes("N/A for Pasted Text") || atsWarnings.includes("ATS check not applicable (historical record)");
        if (wordOrPasted) {
            const labelText = atsWarnings.includes("ATS check not applicable (historical record)") ? "ATS check not applicable (historical record)." : "ATS layout diagnostics not applicable (Word format / Pasted text).";
            atsStatus.innerHTML = `<div class='ats-success-box' style='background-color:#F1F5F9; border-color:#94A3B8; color:#475569; border-left-width: 4px; border-left-style: solid;'>${labelText}</div>`;
        } else {
            const reasons = atsWarnings.join("<br>• ");
            atsStatus.innerHTML = `<div class='ats-warning-box'>
                This resume may have ATS-parsing risks:<br>• ${reasons}<br><br>
                <strong>Consider using a single-column, text-based format for maximum ATS compatibility.</strong>
            </div>`;
        }
    } else {
        atsStatus.innerHTML = `<div class='ats-success-box'>No major ATS parsing risks found for this resume.</div>`;
    }

    // Reset AI Roadmap card
    document.getElementById("roadmapTriggerArea").classList.remove("hidden");
    document.getElementById("roadmapLoading").classList.add("hidden");
    document.getElementById("roadmapDisplay").classList.add("hidden");
    document.getElementById("roadmapDisplay").innerHTML = "";

    // Populate Grammar checker list
    const grammarList = document.getElementById("grammarList");
    grammarList.innerHTML = "";
    if (data.grammarIssues && data.grammarIssues.length > 0) {
        data.grammarIssues.forEach(issue => {
            const item = document.createElement("div");
            item.className = "grammar-item";
            
            let suggestionText = "";
            if (issue.suggestions && issue.suggestions.length > 0) {
                suggestionText = `<div class="grammar-sug">Suggestions: ${issue.suggestions.join(", ")}</div>`;
            }

            item.innerHTML = `
                <div class="grammar-message">${issue.message}</div>
                <div class="grammar-context">"${issue.context}"</div>
                ${suggestionText}
            `;
            grammarList.appendChild(item);
        });
    } else {
        grammarList.innerHTML = "<p class='placeholder-text' style='margin:0;font-size:12px;'>No grammar issues found.</p>";
    }
    switchResultTab('overview');
}

// Chart.js Match Index bar chart mapping
function renderChart(score) {
    if (scoreChart) {
        scoreChart.destroy();
    }

    const canvas = document.getElementById("scoreChart");
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    scoreChart = new Chart(ctx, {
        type: "bar",
        data: {
            labels: ["Match Index"],
            datasets: [{
                data: [score],
                backgroundColor: ["#0D7377"],
                borderColor: ["#0D7377"],
                borderWidth: 0,
                borderRadius: 4,
                barThickness: 16
            }]
        },
        options: {
            indexAxis: "y",
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: { enabled: true }
            },
            scales: {
                x: {
                    min: 0,
                    max: 100,
                    grid: { display: false },
                    ticks: {
                        color: "#718096",
                        font: { family: "Inter", size: 10 }
                    }
                },
                y: {
                    grid: { display: false },
                    display: false
                }
            }
        }
    });
}

// Opt-in Gemini Learning Roadmap Request
function getAIRoadmap() {
    if (!currentAnalysisId) return;

    const triggerArea = document.getElementById("roadmapTriggerArea");
    const roadmapLoading = document.getElementById("roadmapLoading");
    const roadmapDisplay = document.getElementById("roadmapDisplay");

    triggerArea.classList.add("hidden");
    roadmapLoading.classList.remove("hidden");

    fetch(`${API_BASE}/analyze/${currentAnalysisId}/roadmap`, {
        method: "POST",
        headers: {
            "X-Session-Id": sessionId
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Roadmap request failed.");
        }
        return response.json();
    })
    .then(data => {
        roadmapLoading.classList.add("hidden");
        
        const collapsible = document.getElementById("roadmapCollapsible");
        const btn = document.getElementById("toggleRoadmapBtn");
        
        if (collapsible) collapsible.classList.remove("hidden");
        if (btn) {
            btn.classList.remove("hidden");
            btn.textContent = "Hide details";
        }
        roadmapDisplay.classList.remove("hidden");
        
        // Basic Markdown-to-HTML parser mapping for bold text and list highlights
        let formattedText = data.roadmap
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/### (.*?)\n/g, '<h3>$1</h3>')
            .replace(/## (.*?)\n/g, '<h3>$1</h3>')
            .replace(/• (.*?)\n/g, '<li>$1</li>')
            .replace(/\* (.*?)\n/g, '<li>$1</li>')
            .replace(/\n/g, '<br>');

        roadmapDisplay.innerHTML = formattedText;
    })
    .catch(err => {
        roadmapLoading.classList.add("hidden");
        triggerArea.classList.remove("hidden");
        alert("Failed to build roadmap: " + err.message);
    });
}

// Stream PDF Report download
function downloadPdfReport() {
    if (!currentAnalysisId) return;

    fetch(`${API_BASE}/analyze/${currentAnalysisId}/pdf`, {
        headers: {
            "X-Session-Id": sessionId
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Failed to download PDF report. Status: " + response.status);
        }
        return response.blob();
    })
    .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = `resume-insight-report-${currentAnalysisId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
    })
    .catch(err => {
        alert(err.message);
    });
}

// Toggle History Sidebar View
function toggleHistorySidebar() {
    const sidebar = document.getElementById("historySidebar");
    const overlay = document.getElementById("sidebarOverlay");
    
    const isOpen = sidebar.classList.contains("open");
    if (isOpen) {
        sidebar.classList.remove("open");
        overlay.classList.remove("open");
    } else {
        sidebar.classList.add("open");
        overlay.classList.add("open");
        loadHistoryList();
    }
}

let historyTrendChartInstance = null;

function renderHistoryTrendChart(data) {
    const chartContainer = document.getElementById("historyChartContainer");
    if (!data || data.length < 2) {
        chartContainer.classList.add("hidden");
        return;
    }
    chartContainer.classList.remove("hidden");

    // Chronological order (oldest first)
    const sortedData = [...data].sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));

    const labels = sortedData.map(item => {
        const d = new Date(item.timestamp);
        return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
    });
    const scores = sortedData.map(item => item.score);

    const ctx = document.getElementById("historyTrendChart").getContext("2d");
    if (historyTrendChartInstance) {
        historyTrendChartInstance.destroy();
    }

    historyTrendChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Score',
                data: scores,
                borderColor: '#0D7377',
                backgroundColor: 'rgba(13, 115, 119, 0.05)',
                borderWidth: 2,
                pointBackgroundColor: '#0D7377',
                pointRadius: 4,
                tension: 0.2,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) { return `Score: ${context.parsed.y}%`; }
                    }
                }
            },
            scales: {
                y: {
                    min: 0,
                    max: 100,
                    grid: { color: 'rgba(226, 232, 240, 0.6)' },
                    ticks: { font: { size: 9 }, color: '#4A5568' }
                },
                x: {
                    grid: { display: false },
                    ticks: { display: false }
                }
            }
        }
    });
}

// Fetch past analysis logs from backend
function loadHistoryList() {
    const historyList = document.getElementById("historyList");
    historyList.innerHTML = "<p class='placeholder-text' style='text-align:center;'>Loading runs...</p>";

    fetch(`${API_BASE}/history`, {
        headers: {
            "X-Session-Id": sessionId
        }
    })
    .then(response => response.json())
    .then(data => {
        // Render score trend chart
        renderHistoryTrendChart(data);

        historyList.innerHTML = "";
        if (data.length === 0) {
            historyList.innerHTML = "<p class='placeholder-text' style='text-align:center;'>No analysis records saved yet.</p>";
            return;
        }

        data.forEach(item => {
            const date = new Date(item.timestamp);
            const dateFormatted = date.toLocaleString();
            const historyItem = document.createElement("div");
            historyItem.className = "history-item";
            
            const scoreClass = item.score >= 70 ? "score-high" : (item.score >= 40 ? "score-mid" : "score-low");
            historyItem.innerHTML = `
                <div class="history-item-header">
                    <span class="history-file" title="${item.filename}">${item.filename}</span>
                    <span class="history-score ${scoreClass}">${item.score}%</span>
                </div>
                <div class="history-meta">
                    <div>Mode: ${item.jobDescription ? "JD Match" : "Role list"}</div>
                    <div>${dateFormatted}</div>
                </div>
            `;
            
            // Reload historical analysis state on click
            historyItem.addEventListener("click", () => {
                reloadHistoryState(item);
                toggleHistorySidebar();
            });

            historyList.appendChild(historyItem);
        });
    })
    .catch(err => {
        historyList.innerHTML = `<p class='placeholder-text' style='color:#DC2626;'>Failed to load history: ${err.message}</p>`;
    });
}

// Delete current session history records
function clearSessionHistory() {
    if (!confirm("Are you sure you want to clear your history?")) {
        return;
    }

    fetch(`${API_BASE}/history`, {
        method: "DELETE",
        headers: {
            "X-Session-Id": sessionId
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error("Clear history request failed.");
        }
        return response.json();
    })
    .then(data => {
        // Reload list to update sidebar and chart
        loadHistoryList();
    })
    .catch(err => {
        console.error(err);
        alert("Failed to clear history: " + err.message);
    });
}

// Reload previous run state into form and dashboard
function reloadHistoryState(item) {
    clearInputs();
    
    // Hide AI hero section and show results wrapper
    const aiHeroSection = document.getElementById("aiHeroSection");
    if (aiHeroSection) {
        aiHeroSection.classList.add("hidden");
    }
    const resultsSectionWrapper = document.getElementById("resultsSectionWrapper");
    if (resultsSectionWrapper) {
        resultsSectionWrapper.classList.remove("hidden");
    }
    
    resumeText.value = item.resumeText;
    updateCharacterCount();

    if (item.jobDescription) {
        analysisMode.value = "jd";
        jobDescriptionText.value = item.jobDescription;
        jdGroup.classList.remove("hidden");
        roleDropdownGroup.classList.add("hidden");
    } else {
        analysisMode.value = "role";
        roleDropdownGroup.classList.remove("hidden");
        jdGroup.classList.add("hidden");
        roleDropdown.value = "all";
    }

    currentAnalysisId = item.id;
    uploadedFilename = item.filename === "Pasted Resume Text" ? null : item.filename;
    if (uploadedFilename) {
        fileStatus.textContent = `PDF Loaded: ${uploadedFilename}`;
    }

    // Prepare JSON object mapping to simulate active results response
    const mockResponse = {
        id: item.id,
        text: item.resumeText,
        score: item.score,
        bestRoleRecommendation: item.bestRole,
        matchedSkills: item.matchedSkills ? item.matchedSkills.split(",") : [],
        missingSkills: item.missingSkills ? item.missingSkills.split(",") : [],
        matchedSoftSkills: item.matchedSoftSkills ? item.matchedSoftSkills.split(",") : [],
        missingSoftSkills: item.missingSoftSkills ? item.missingSoftSkills.split(",") : [],
        missingSkillsPrioritized: [],
        jdMatchMode: !!item.jobDescription,
        grammarIssues: [],
        roleRanking: [],
        atsWarnings: (item.atsWarnings && item.atsWarnings.trim()) ? item.atsWarnings.split(";") : []
    };

    // Populate mocked priority list for reloading
    if (item.missingSkills) {
        item.missingSkills.split(",").forEach(s => {
            if (s.trim()) {
                mockResponse.missingSkillsPrioritized.push({
                    name: s.trim(),
                    priority: item.jobDescription ? "Critical" : "Nice to have"
                });
            }
        });
    }
    if (item.missingSoftSkills) {
        item.missingSoftSkills.split(",").forEach(s => {
            if (s.trim()) {
                mockResponse.missingSkillsPrioritized.push({
                    name: s.trim(),
                    priority: item.jobDescription ? "Critical" : "Nice to have"
                });
            }
        });
    }

    // Load grammar issues list safely
    try {
        if (item.grammarIssuesJson) {
            mockResponse.grammarIssues = JSON.parse(item.grammarIssuesJson);
        }
    } catch(e) {
        console.error("Error parsing history grammarJson: ", e);
    }

    renderResults(mockResponse);

    // If roadmap exists, display it immediately
    if (item.roadmap) {
        document.getElementById("roadmapTriggerArea").classList.add("hidden");
        const roadmapDisplay = document.getElementById("roadmapDisplay");
        const collapsible = document.getElementById("roadmapCollapsible");
        const btn = document.getElementById("toggleRoadmapBtn");
        
        if (collapsible) collapsible.classList.remove("hidden");
        if (btn) {
            btn.classList.remove("hidden");
            btn.textContent = "Hide details";
        }
        roadmapDisplay.classList.remove("hidden");

        let formattedText = item.roadmap
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/### (.*?)\n/g, '<h3>$1</h3>')
            .replace(/## (.*?)\n/g, '<h3>$1</h3>')
            .replace(/• (.*?)\n/g, '<li>$1</li>')
            .replace(/\* (.*?)\n/g, '<li>$1</li>')
            .replace(/\n/g, '<br>');

        roadmapDisplay.innerHTML = formattedText;
    }
}

// Tagline Rotation Logic
function initTaglineRotation() {
    const taglines = document.querySelectorAll("#aiHeroSection .tagline");
    if (taglines.length === 0) return;
    
    let currentIndex = 0;
    
    function rotateTaglines() {
        const currentTagline = taglines[currentIndex];
        currentTagline.classList.remove("active");
        currentTagline.classList.add("fade-out");
        
        setTimeout(() => {
            currentTagline.classList.remove("fade-out");
            currentIndex = (currentIndex + 1) % taglines.length;
            
            const nextTagline = taglines[currentIndex];
            nextTagline.classList.add("active");
        }, 600); // match transition duration in CSS
    }
    
    // Rotate every 3.6s (3s visibility + 0.6s transition)
    setInterval(rotateTaglines, 3600);
}

// Fetch dynamic roles from backend on startup
document.addEventListener("DOMContentLoaded", () => {
    initTaglineRotation();
    
    fetch(`${API_BASE}/roles`)
    .then(response => {
        if (!response.ok) {
            throw new Error("Failed to load roles from backend");
        }
        return response.json();
    })
    .then(roles => {
        const select = document.getElementById("roleDropdown");
        if (select) {
            select.innerHTML = '<option value="all">Analyze All Roles</option>';
            roles.forEach(role => {
                const opt = document.createElement("option");
                opt.value = role;
                opt.textContent = role;
                select.appendChild(opt);
            });
        }
    })
    .catch(err => console.error("Error loading roles:", err));
});

const WEAK_VERB_PHRASES = [
    "worked on", "helped with", "responsible for", "involved in", "assisted with", "participated in", "handled"
];

function checkResumeStructureClient(text) {
    const suggestions = [];
    if (!text) return suggestions;
    const ut = text.toUpperCase();

    if (!ut.includes("EMAIL") && !ut.includes("PHONE") && !ut.includes("LINKEDIN") && !ut.includes("CONTACT")) {
        suggestions.push("No dedicated Contact Info section or contact keywords (Email, Phone) detected.");
    }
    if (!ut.includes("EDUCATION") && !ut.includes("ACADEMIC")) {
        suggestions.push("Consider adding an Education section to list your academic background.");
    }
    if (!ut.includes("SKILLS") && !ut.includes("TECHNOLOGIES") && !ut.includes("TECHNICAL SKILLS")) {
        suggestions.push("No dedicated Skills section detected. Make your competencies easily scanable.");
    }
    if (!ut.includes("PROJECTS") && !ut.includes("EXPERIENCE") && !ut.includes("WORK") && !ut.includes("EMPLOYMENT")) {
        suggestions.push("Consider adding a Projects or Experience section to showcase your hands-on work.");
    }
    if (!ut.includes("CERTIFICATIONS") && !ut.includes("CERTIFICATES") && !ut.includes("CREDENTIALS")) {
        suggestions.push("Consider adding a Certifications section to display your credentials.");
    }

    const words = text.trim().split(/\s+/);
    if (words.length > 1000) {
        suggestions.push(`Your resume length suggests more than 2 pages of content (${words.length} words). Consider condensing it to 1-2 pages.`);
    }

    return suggestions;
}

function checkBulletPointsClient(text) {
    const feedback = [];
    if (!text) return feedback;
    const lines = text.split(/\r?\n/);
    lines.forEach(line => {
        const trimmed = line.trim();
        if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("–") || trimmed.startsWith("—") || trimmed.startsWith("*")) {
            const bulletText = trimmed.substring(1).trim();
            if (!bulletText) return;

            const bulletSuggestions = [];
            const hasMetric = /\d+/.test(bulletText) || bulletText.toLowerCase().includes("percent") || bulletText.toLowerCase().includes("percentage");
            if (!hasMetric) {
                bulletSuggestions.push("Consider adding measurable impact to this point (e.g. a percentage, number, or scale).");
            }

            const lowerBullet = bulletText.toLowerCase();
            let foundWeakPhrase = null;
            for (let phrase of WEAK_VERB_PHRASES) {
                if (lowerBullet.startsWith(phrase)) {
                    foundWeakPhrase = phrase;
                    break;
                }
            }
            if (foundWeakPhrase) {
                bulletSuggestions.push(`Starts with passive phrase '${foundWeakPhrase}'. Suggest using a stronger action verb alternative (e.g. 'Developed', 'Led', 'Optimized', 'Implemented', 'Designed').`);
            }

            if (bulletSuggestions.length > 0) {
                feedback.push({
                    originalBullet: trimmed,
                    suggestions: bulletSuggestions
                });
            }
        }
    });
    return feedback;
}

function toggleGrammarDetails() {
    const content = document.getElementById("grammarContent");
    const btn = document.getElementById("toggleGrammarBtn");
    if (!content || !btn) return;
    if (content.classList.contains("hidden")) {
        content.classList.remove("hidden");
        btn.textContent = "Hide details";
    } else {
        content.classList.add("hidden");
        btn.textContent = "Show details";
    }
}

function toggleRoadmapDetails() {
    const content = document.getElementById("roadmapCollapsible");
    const btn = document.getElementById("toggleRoadmapBtn");
    if (!content || !btn) return;
    if (content.classList.contains("hidden")) {
        content.classList.remove("hidden");
        btn.textContent = "Hide details";
    } else {
        content.classList.add("hidden");
        btn.textContent = "Show details";
    }
}

// Toggle Hamburger Menu
function toggleMenu(event) {
    if (event) {
        event.stopPropagation();
    }
    const menu = document.getElementById("menuDropdown");
    if (menu) {
        if (menu.classList.contains("hidden")) {
            menu.classList.remove("hidden");
        } else {
            menu.classList.add("hidden");
        }
    }
}

// Menu Action: View History
function menuViewHistory(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    // Close menu first
    const menu = document.getElementById("menuDropdown");
    if (menu) menu.classList.add("hidden");
    
    // Toggle history sidebar
    toggleHistorySidebar();
}

// Menu Action: Clear Local Cache
function menuClearCache(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    // Close menu first
    const menu = document.getElementById("menuDropdown");
    if (menu) menu.classList.add("hidden");

    if (confirm("Reset current upload states and clear inputs?")) {
        clearInputs();
    }
}

// Close menu when clicking outside
document.addEventListener("click", (event) => {
    const menu = document.getElementById("menuDropdown");
    const menuBtn = document.getElementById("menuBtn");
    if (menu && !menu.classList.contains("hidden")) {
        if (!menu.contains(event.target) && event.target !== menuBtn) {
            menu.classList.add("hidden");
        }
    }
});

// Switch results display tabs
function switchResultTab(tabId) {
    const panes = document.querySelectorAll(".tab-pane");
    panes.forEach(pane => pane.classList.add("hidden"));

    const buttons = document.querySelectorAll(".tab-btn");
    buttons.forEach(btn => btn.classList.remove("active"));

    const activePane = document.getElementById(`tab-${tabId}`);
    if (activePane) activePane.classList.remove("hidden");

    const activeBtn = Array.from(buttons).find(btn => btn.getAttribute("onclick").includes(`'${tabId}'`));
    if (activeBtn) activeBtn.classList.add("active");
}
