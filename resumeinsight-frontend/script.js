function analyzeResume() {
    const resumeText = document.getElementById("resumeText").value;

    if (resumeText.trim() === "") {
        alert("Please paste resume text first");
        return;
    }

    document.getElementById("loadingMessage").style.display = "block";
    document.getElementById("resultSection").style.display = "none";

    fetch("http://localhost:8080/analyze", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
        text: resumeText,
        selectedRole: getSelectedRole()
        })
    })
    .then(response => response.json())
    .then(data => {

        setTimeout(() => {

            document.getElementById("loadingMessage").style.display = "none";
            document.getElementById("resultSection").style.display = "block";

            const skillsList = document.getElementById("skillsList");
            const roleRanking = document.getElementById("roleRanking");
            const bestRole = document.getElementById("bestRole");
            const feedbackText = document.getElementById("feedbackText");

            skillsList.innerHTML = "";
            roleRanking.innerHTML = "";
            bestRole.innerHTML = "";
            feedbackText.innerHTML = "";

           bestRole.innerHTML = data.bestRoleRecommendation.replace(/\n/g, "<br>");
           const topScore = data.roleRanking[0].score;

             if (data.bestRoleRecommendation.includes("No strong role match")) {

             feedbackText.textContent =
             "Your resume currently does not strongly match any supported technical role.Add more domain-specific skills and build relevant projects.";

    }
     else if (topScore === 100) {

            feedbackText.textContent =
            "Excellent match! Your resume strongly fits this role. Focus on polishing projects and preparing for interviews.";

    }
        else if (topScore >= 70) {

        feedbackText.textContent =
        "Good match. Improve a few missing skills and strengthen 1-2 strong role-based projects.";

    }
        else if (topScore >= 40) {

         feedbackText.textContent =
         "Moderate match. Work on missing core skills and build role-specific projects.";

}
    else {

            feedbackText.textContent =
            "Weak match. Start by learning important skills required for this role.";

}

            const bestRoleCard = document.querySelector(".best-role-card");

            if (data.bestRoleRecommendation.includes("No strong role match")) {
                bestRoleCard.style.background = "linear-gradient(135deg, #f59e0b, #d97706)";
            } else {
                bestRoleCard.style.background = "linear-gradient(135deg, #2563eb, #1e40af)";
            }

            data.detectedSkills.forEach(skill => {
                const skillBadge = document.createElement("span");
                skillBadge.className = "skill-badge";
                skillBadge.textContent = skill;
                skillsList.appendChild(skillBadge);
            });

            data.roleRanking.forEach(role => {
                const roleCard = document.createElement("div");

                if (role.score === data.roleRanking[0].score) {
                    roleCard.className = "role-card top-role";
        } else {
                    roleCard.className = "role-card";
    }

                roleCard.innerHTML = `
                    <div class="role-info">
                        <span class="role-name">${role.role}</span>
                        <span class="role-score">${role.score}%</span>
                    </div>

                    <div class="progress-bar">
                        <div class="progress-fill"></div>
                    </div>
                `;

                roleRanking.appendChild(roleCard);
                setTimeout(() => {
                 roleCard.querySelector(".progress-fill").style.width = role.score + "%";
                }, 800);
            });

        }, 500);

    })
    .catch(error => {
        alert("Error connecting to backend");
        console.log(error);
    });
}
 function clearResume() {
    document.getElementById("resumeText").value = "";
    document.getElementById("resultSection").style.display = "none";
    document.getElementById("loadingMessage").style.display = "none";

    document.getElementById("charCount").textContent = "Characters: 0 / Recommended: 300-1500";
    document.getElementById("charCount").style.color = "#6b7280";
}

const resumeTextArea = document.getElementById("resumeText");
const charCount = document.getElementById("charCount");

function updateCharacterCount() {
    const count = resumeTextArea.value.length;

    charCount.textContent =
        "Characters: " + count + " / Recommended: 300-1500";

    if (count > 0 && count < 300) {
        charCount.style.color = "#f59e0b";
    }
    else if (count > 1500) {
        charCount.style.color = "#dc2626";
    }
    else {
        charCount.style.color = "#6b7280";
    }
}

resumeTextArea.addEventListener("input", updateCharacterCount);

resumeTextArea.addEventListener("cut", function () {
    setTimeout(updateCharacterCount, 0);
});

resumeTextArea.addEventListener("paste", function () {
    setTimeout(updateCharacterCount, 0);
});

document.getElementById("resumeText").addEventListener("keydown", function(event) {

    if (event.ctrlKey && event.key === "Enter") {
        analyzeResume();
    }

});

function getSelectedRole() {
    const analysisMode = document.getElementById("analysisMode").value;
    const roleDropdown = document.getElementById("roleDropdown");

    if (analysisMode === "selected") {
        return roleDropdown.value;
    }

    return null;
}

const analysisModeDropdown = document.getElementById("analysisMode");
const roleDropdown = document.getElementById("roleDropdown");

analysisModeDropdown.addEventListener("change", function () {

    if (analysisModeDropdown.value === "selected") {
        roleDropdown.classList.remove("hidden");
    } else {
        roleDropdown.classList.add("hidden");
    }

});

function downloadReport() {

    const bestRole = document.getElementById("bestRole").innerText;
    const skills = document.getElementById("skillsList").innerText;
    const ranking = document.getElementById("roleRanking").innerText;

    const report =
        "Resume Insight Report\n\n" +
        "Best Role Recommendation:\n" + bestRole + "\n\n" +
        "Detected Skills:\n" + skills + "\n\n" +
        "Role Ranking:\n" + ranking;

    const blob = new Blob([report], { type: "text/plain" });

    const link = document.createElement("a");

    link.href = URL.createObjectURL(blob);
    link.download = "resume-insight-report.txt";

    link.click();
}