const root = document.querySelector('#root');
const API = '/api';
const icons = { dashboard:'◫', profile:'◎', resumes:'▤', analysis:'✳', interviews:'◉', history:'◷', matching:'⌘', plan:'▦', settings:'⚙' };
const nav = [
  ['dashboard','/dashboard','Overview'],['profile','/profile','My profile'],['resumes','/resume','Resume library'],
  ['analysis','/resume-analysis','Resume insights'],['interviews','/interviews','Mock interviews'],
  ['history','/interview-history','Interview history'],['matching','/job-matching','Role matching'],
  ['plan','/study-plan','Study plan'],['settings','/settings','Settings']
];
let appState = { user:null, drawer:false, loadError:null };
function esc(value=''){return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function initials(name=''){return name.trim().split(/\s+/).slice(0,2).map(x=>x[0]||'').join('').toUpperCase()||'CS';}
function dateLabel(date){return date?new Date(date).toLocaleDateString(undefined,{month:'short',day:'numeric',year:'numeric'}):'—';}
function interviewScore(value){return value==null?'—':`${Number(value).toFixed(1)} / 10`;}
function safeArray(value){return Array.isArray(value)?value:[];}
async function request(path, options={}){
  const headers = new Headers(options.headers||{});
  if(options.body && !(options.body instanceof FormData)) headers.set('Content-Type','application/json');
  const method=(options.method||'GET').toUpperCase();
  if(!['GET','HEAD','OPTIONS','TRACE'].includes(method)){
    const csrfResponse=await fetch(`${API}/auth/csrf`,{credentials:'include'});
    if(!csrfResponse.ok)throw new Error('Could not verify this request. Refresh the page and try again.');
    const csrf=await csrfResponse.json();
    headers.set('X-XSRF-TOKEN',csrf.token);
  }
  const response=await fetch(`${API}${path}`,{...options,headers,credentials:'include'});
  const text=await response.text(); let payload={};
  try{payload=text?JSON.parse(text):{};}catch{payload={message:text||'The server returned an unreadable response.'};}
  if(!response.ok){
    if(response.status===401&&!path.startsWith('/auth/')){
      appState.user=null;
      if(!['/login','/register'].includes(currentRoute()))setTimeout(()=>navigate('/login'),0);
    }
    const error=new Error(payload.message||`Request failed (${response.status})`);error.status=response.status;error.fieldErrors=payload.fieldErrors||{};throw error;
  }
  return payload;
}
function toast(message){const old=document.querySelector('.toast');if(old)old.remove();const el=document.createElement('div');el.className='toast';el.setAttribute('role','status');el.textContent=message;document.body.append(el);setTimeout(()=>el.remove(),3400);}
function navigate(path){history.pushState({},'',path);appState.drawer=false;render();}
function button(label, action, kind='primary', extra=''){return `<button class="button ${kind} ${extra}" type="button" data-action="${action}">${label}</button>`;}
function pageHead(eyebrow,title,sub,action=''){return `<div class="welcome-line"><div><p class="eyebrow">${eyebrow}</p><h1 class="page-title">${title}</h1><p class="subhead">${sub}</p></div>${action}</div>`;}
function empty(title,description,action=''){return `<div class="empty-state"><div class="empty-mark">⌁</div><strong>${title}</strong><p>${description}</p>${action}</div>`;}
function loadingCards(count=3){return `<div class="grid stats-grid">${Array.from({length:count},()=>'<div class="skeleton"></div>').join('')}</div>`;}
function apiError(error,retry='retry'){return `<div class="notice" role="alert"><strong>We couldn’t load this just yet.</strong><br>${esc(error?.message||'Please try again in a moment.')}<div style="margin-top:12px">${button('Try again',retry,'secondary','small')}</div></div>`;}
function layout(content,active){
  const user=appState.user||{};
  return `<div class="shell"><aside class="sidebar ${appState.drawer?'open':''}" aria-label="Main navigation">
    <a class="brand" href="/dashboard" data-route="/dashboard"><span class="brandmark">C</span><span>CareerSphere<small>YOUR CAREER, IN MOTION</small></span></a>
    <div class="nav-label">Workspace</div><nav class="nav-list">${nav.map(([key,url,label])=>`<a class="nav-link ${key===active?'active':''}" href="${url}" data-route="${url}" ${key===active?'aria-current="page"':''}><span class="icon">${icons[key]}</span>${label}</a>`).join('')}</nav>
    <div class="side-bottom"><div class="sidebar-note"><strong>Small steps count.</strong><p>Your next opportunity is built one focused session at a time.</p></div><div class="user-mini"><div class="avatar">${esc(initials(user.fullName))}</div><div><strong>${esc(user.fullName||'Your workspace')}</strong><span>${esc(user.targetRole||user.email||'Set your target role')}</span></div></div></div>
  </aside><div class="mobile-overlay ${appState.drawer?'show':''}" data-action="close-menu"></div><section class="main"><header class="topbar"><div style="display:flex;align-items:center;gap:12px"><button class="menu-toggle" type="button" aria-label="Open navigation" data-action="toggle-menu">☰</button><span class="crumb">Workspace <span style="color:#47627a;padding:0 6px">/</span> <b>${esc(nav.find(n=>n[0]===active)?.[2]||'Overview')}</b></span></div><div class="top-actions"><span class="date-chip">${new Date().toLocaleDateString(undefined,{weekday:'short',month:'short',day:'numeric'})}</span><button class="icon-btn" type="button" aria-label="Settings" data-route="/settings">⚙</button></div></header><main class="main-content">${content}</main></section></div>`;
}
function authView(register=false){
  return `<div class="auth-layout"><aside class="auth-aside"><a class="brand" href="/login" data-route="/login"><span class="brandmark">C</span><span>CareerSphere<small>YOUR CAREER, IN MOTION</small></span></a><div><p class="eyebrow">Build what comes next</p><h1>Your next chapter starts <span>here.</span></h1><p>A focused home for the skills, practice, and small wins that move your career forward.</p></div><div class="auth-quote">“You don’t need the whole map. Just a clear next step.”</div></aside><section class="auth-form-side"><form class="auth-form" data-form="${register?'register':'login'}"><p class="eyebrow">${register?'A workspace of your own':'Welcome back'}</p><h2>${register?'Create your account':'Sign in to CareerSphere'}</h2><p>${register?'Start with the direction you’re heading. You can refine the details anytime.':'Pick up where you left off and keep your momentum.'}</p>
  ${register?`<div class="field"><label for="fullName">Full name</label><input id="fullName" name="fullName" required minlength="2" autocomplete="name" placeholder="Your name"><span class="field-error" data-error="fullName"></span></div>`:''}
  <div class="field"><label for="email">Email address</label><input id="email" name="email" required type="email" autocomplete="email" placeholder="you@example.com"><span class="field-error" data-error="email"></span></div>
  <div class="field"><label for="password">Password</label><input id="password" name="password" required type="password" minlength="${register?10:1}" autocomplete="${register?'new-password':'current-password'}" placeholder="${register?'At least 10 characters':'Enter your password'}"><span class="field-error" data-error="password"></span></div>
  ${register?`<div class="field"><label for="targetRole">Target role <span class="muted">(optional)</span></label><input id="targetRole" name="targetRole" placeholder="e.g. Frontend Engineer"><span class="field-error" data-error="targetRole"></span></div><div class="field"><label for="experienceLevel">Experience level</label><select name="experienceLevel" id="experienceLevel"><option>Fresher</option><option selected>Entry-level</option><option>Mid-level</option></select></div>`:''}
  <div class="field-error" data-error="form"></div><button class="button primary" style="width:100%;margin-top:7px" type="submit">${register?'Create account':'Sign in'} <span>→</span></button><div class="auth-foot">${register?'Already have an account?':'New to CareerSphere?'} <a href="${register?'/login':'/register'}" data-route="${register?'/login':'/register'}">${register?'Sign in':'Create an account'}</a></div>
  </form></section></div>`;
}
function dashboardPage(){
 return `<div id="page-data">${pageHead('YOUR CAREER AT A GLANCE',`Good ${new Date().getHours()<12?'morning':new Date().getHours()<18?'afternoon':'evening'}, ${esc((appState.user?.fullName||'there').split(' ')[0])}.`,'A little progress today makes a bigger difference tomorrow.')}<div class="dashboard-loading">${loadingCards(4)}</div></div>`;
}
function dashboardMarkup(data){
 const interviews=safeArray(data.recentInterviews),skills=safeArray(data.recommendedSkills),gaps=safeArray(data.skillGaps);
 const stat=(label,value,foot,symbol,tone)=>`<article class="panel stat-card" style="--tone:${tone}"><div class="stat-top">${label}<span class="stat-symbol">${symbol}</span></div><div class="stat-value">${value}</div><div class="stat-foot">${foot}</div></article>`;
 return `<div>${pageHead('YOUR CAREER AT A GLANCE',`Good ${new Date().getHours()<12?'morning':new Date().getHours()<18?'afternoon':'evening'}, ${esc((appState.user?.fullName||'there').split(' ')[0])}.`,'A little progress today makes a bigger difference tomorrow.',button('＋ Update profile','go-profile','secondary'))}
 <div class="grid stats-grid">${stat('Resume assessments',data.resumesAnalyzed??0,'Documents reviewed','▤','#65d9e8')}${stat('Practice interviews',data.interviewsCompleted??0,'Sessions completed','◉','#83aaf5')}${stat('Interview average',interviewScore(data.averageInterviewScore),'Across completed sessions','↗','#89dfc1')}${stat('Career direction',esc(data.targetRole||'Not set'),'Your current target role','◎','#f2c778')}</div>
 <div class="grid content-grid"><div class="grid"><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Keep your momentum</h2><span class="panel-meta">Choose one next step</span></div><div class="grid quick-grid"><button class="quick-action" data-route="/resume"><span class="qa-icon">▤</span><strong>Review your resume</strong><span>Upload a PDF and see focused suggestions</span></button><button class="quick-action" data-route="/interviews"><span class="qa-icon">◉</span><strong>Practice an interview</strong><span>Build confidence one answer at a time</span></button><button class="quick-action" data-route="/job-matching"><span class="qa-icon">⌘</span><strong>Explore role fit</strong><span>See where your skills line up</span></button><button class="quick-action" data-route="/study-plan"><span class="qa-icon">▦</span><strong>Continue learning</strong><span>Pick up your weekly roadmap</span></button></div></section>
  <section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Recent interview practice</h2><a href="/interview-history" data-route="/interview-history" class="panel-meta">View history →</a></div>${interviews.length?`<div class="table-wrap"><table><thead><tr><th>Role</th><th>Difficulty</th><th>Progress</th><th>Score</th></tr></thead><tbody>${interviews.slice(0,5).map(i=>`<tr><td><a href="/interviews/${i.id}" data-route="/interviews/${i.id}">${esc(i.role)}</a></td><td>${esc(i.difficulty)}</td><td><span class="badge ${i.status==='COMPLETED'?'green':'amber'}">${i.status==='COMPLETED'?'Completed':`${i.answeredCount||0}/${i.questionCount} answered`}</span></td><td>${interviewScore(i.overallScore)}</td></tr>`).join('')}</tbody></table></div>`:empty('Your practice starts here','Complete a mock interview to see your progress and feedback.',button('Start an interview','go-interviews','secondary','small'))}</section></div>
 <div class="grid"><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Skills to build</h2><span class="badge">${gaps.length} focus areas</span></div>${gaps.length?gaps.slice(0,5).map((skill,i)=>`<div class="skill-row"><span>${esc(skill)}</span><div class="progress-line"><i style="width:${Math.max(14,76-i*11)}%"></i></div><em>${i===0?'Next':'Soon'}</em></div>`).join(''):`<p class="subhead">No skill gaps to show yet. Add your skills and target role to make this useful.</p>`}</section>
 <section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Recommended to explore</h2><span class="panel-meta">${esc(data.assessmentMode||'')}</span></div>${skills.length?`<div class="tag-list">${skills.slice(0,10).map(s=>`<span class="tag">${esc(s)}</span>`).join('')}</div>`:empty('Recommendations are on the way','Set a target role and add skills to your profile.')}${data.assessmentMode==='DEMO'?'<div class="notice-demo">DEMO insight · use as a starting point, not a prediction.</div>':''}</section></div></div></div>`;
}
function profilePage(){
 return `<div>${pageHead('YOUR FOUNDATION','Professional profile','Keep your background and skills current. Your profile helps tailor each next step.')}<div class="panel form-card" id="profile-content"><div class="skeleton"></div></div></div>`;
}
function profileMarkup(user){
 const skills=safeArray(user.skills);
 return `<form class="form-card panel" data-form="profile"><div class="form-grid">
 ${inputField('Full name','fullName',user.fullName||'',true)}${inputField('Email address','email',user.email||'',true,'email','disabled')}
 ${inputField('Phone','phone',user.phone||'')}${inputField('Location','location',user.location||'')}
 ${inputField('Education','education',user.education||'')}${inputField('Graduation year','graduationYear',user.graduationYear||'','number')}
 <div class="field"><label for="targetRole">Target role</label><input id="targetRole" name="targetRole" value="${esc(user.targetRole||'')}" placeholder="e.g. Software Engineer"></div>
 <div class="field"><label for="experienceLevel">Experience level</label><select id="experienceLevel" name="experienceLevel">${['Fresher','Entry-level','Mid-level'].map(x=>`<option ${user.experienceLevel===x?'selected':''}>${x}</option>`).join('')}</select></div>
 ${inputField('GitHub profile','githubUrl',user.githubUrl||'','url')}${inputField('LinkedIn profile','linkedinUrl',user.linkedinUrl||'','url')}
 <div class="field full"><label for="skills">Skills</label><input id="skills" name="skills" value="${esc(skills.join(', '))}" placeholder="JavaScript, React, SQL"><span class="field-help">Separate skills with commas. Keep them specific.</span></div>
 </div><div class="field-error" data-error="form"></div><div class="form-actions"><button class="button primary" type="submit">Save profile <span>→</span></button></div></form>`;
}
function inputField(label,name,value='',required=false,type='text',extra=''){if(typeof required==='string'){extra=type;type=required;required=false;}return `<div class="field"><label for="${name}">${label}</label><input id="${name}" name="${name}" type="${type}" value="${esc(value)}" ${required?'required':''} ${extra} placeholder="${label}"></div>`;}
function resumesPage(){return `<div>${pageHead('DOCUMENTS','Resume library','A clear view of the versions you’ve shared and what each one reveals.',button('＋ Upload resume','focus-upload','primary'))}<section class="panel panel-pad mb-16"><div class="panel-head"><h2 class="panel-title">Add a resume</h2><span class="panel-meta">PDF only · your uploads stay private</span></div><form data-form="upload" class="form-grid"><label class="dropzone field full" for="resume-file"><span style="font-size:22px;color:var(--cyan)">↑</span><strong>Choose a PDF to upload</strong><span data-file-label>Maximum file size depends on your workspace</span><input id="resume-file" name="file" type="file" accept="application/pdf,.pdf" required hidden></label><div class="field"><label for="resume-role">Target role</label><input id="resume-role" name="targetRole" value="${esc(appState.user?.targetRole||'')}" placeholder="e.g. Backend Engineer" required></div><div class="field" style="align-self:end"><button class="button primary" type="submit">Upload and review <span>→</span></button></div><div class="field-error full" data-error="form"></div></form></section><section id="resumes-list" class="panel panel-pad"><div class="skeleton"></div></section></div>`;}
function resumeListMarkup(items){
 if(!items.length)return empty('No resumes yet','Add a PDF resume above to get role-specific feedback and a baseline for your next edit.');
 return `<div class="panel-head"><h2 class="panel-title">Your resumes</h2><span class="panel-meta">${items.length} ${items.length===1?'document':'documents'}</span></div><div class="table-wrap"><table><thead><tr><th>Document</th><th>Target role</th><th>Added</th><th>Career alignment</th><th></th></tr></thead><tbody>${items.map(r=>`<tr><td>${esc(r.fileName)}</td><td>${esc(r.targetRole)}</td><td>${dateLabel(r.createdAt)}</td><td><span class="badge ${r.assessmentMode==='DEMO'?'amber':'green'}">${r.overallScore??'—'}% insight score</span></td><td><button class="button secondary small" data-resume="${r.id}">View insights →</button></td></tr>`).join('')}</tbody></table></div><p class="panel-meta" style="margin:14px 0 0">Scores are guidance for improving your document—not an employer ATS score.</p>`;
}
function resumeAnalysisPage(){
 const id=new URLSearchParams(location.search).get('id');
 return `<div>${pageHead('RESUME INSIGHTS','A more focused resume','Review strengths, find skill gaps, and turn specific notes into stronger language.')}${id?`<section id="analysis-content" class="panel panel-pad"><div class="skeleton"></div></section>`:`<section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Choose a resume to review</h2><a data-route="/resume" href="/resume" class="panel-meta">Resume library →</a></div><div id="analysis-picker"><div class="skeleton"></div></div></section>`}<section class="panel panel-pad mt-16"><div class="panel-head"><h2 class="panel-title">Improve a section</h2><span class="panel-meta">AI suggestions are a starting point; keep your own voice.</span></div><form class="form-grid" data-form="improve"><div class="field"><label for="section">Section</label><select name="section" id="section"><option value="SUMMARY">Professional summary</option><option value="PROJECT">Project</option><option value="EXPERIENCE">Experience</option><option value="BULLET">Bullet point</option></select></div><div class="field"><label for="goal">What should it emphasize?</label><input name="goal" id="goal" placeholder="e.g. measurable impact and ownership"></div><div class="field full"><label for="originalText">Your original text</label><textarea name="originalText" id="originalText" required maxlength="5000" placeholder="Paste the section you’d like to strengthen…"></textarea></div><div class="field-error full" data-error="form"></div><div class="field full"><button class="button primary" type="submit">Get a focused suggestion →</button></div><div id="improvement-result" class="field full"></div></form></section></div>`;
}
function analysisMarkup(detail){
 const a=detail.analysis||detail;
 const list=(title,items)=>`<section class="panel panel-pad"><div class="panel-head"><h3 class="panel-title">${title}</h3></div>${safeArray(items).length?`<ul class="list-clean">${items.map(x=>`<li>${esc(x)}</li>`).join('')}</ul>`:`<p class="panel-meta">Nothing to show yet.</p>`}</section>`;
 return `<div class="analysis-top"><div class="score-ring" style="--score:${Number(a.overallScore)||0}%"><strong>${a.overallScore??'—'}</strong></div><div><p><b>${esc(detail.fileName||'Resume assessment')}</b></p><p>Career alignment insight · ${a.overallScore??'—'} / 100</p><p>Document structure signal · ${a.atsScore??'—'} / 100</p><p class="panel-meta">These are guidance scores for your document—not a score from any employer’s ATS.</p></div></div>${a.assessmentMode==='DEMO'?'<div class="notice-demo">DEMO assessment · suggestions are illustrative and not generated by Gemini.</div>':''}<div class="grid two-col mt-16">${list('Strengths',a.strengths)}${list('Areas to strengthen',a.weaknesses)}${list('Skills to highlight',a.technicalSkills)}${list('Skills to consider',a.missingSkills)}${list('Suggested edits',a.suggestions)}${list('Project ideas',a.projectSuggestions)}</div><section class="panel panel-pad mt-16"><div class="panel-head"><h3 class="panel-title">Keywords & technologies</h3></div><div class="tag-list">${[...safeArray(a.keywords),...safeArray(a.recommendedTechnologies)].map(x=>`<span class="tag">${esc(x)}</span>`).join('')||'<span class="panel-meta">No keywords available.</span>'}</div><p class="panel-meta mt-16">Suggested roles: ${safeArray(a.recommendedRoles).map(esc).join(' · ')||'—'}</p></section>`;
}
function interviewsPage(){return `<div>${pageHead('PRACTICE WITH PURPOSE','Mock interviews','A calm place to think out loud, practice your reasoning, and learn from each answer.')}<div class="grid content-grid"><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Set up a session</h2><span class="badge">Your pace, your practice</span></div><form class="form-grid" data-form="create-interview"><div class="field full"><label for="role">Interview role</label><input id="role" name="role" required value="${esc(appState.user?.targetRole||'')}" placeholder="e.g. Junior Frontend Engineer"></div><div class="field"><label for="experienceLevel">Experience level</label><select id="experienceLevel" name="experienceLevel">${['Fresher','Entry-level','Mid-level'].map(v=>`<option ${appState.user?.experienceLevel===v?'selected':''}>${v}</option>`).join('')}</select></div><div class="field"><label for="difficulty">Difficulty</label><select id="difficulty" name="difficulty"><option>Easy</option><option selected>Medium</option><option>Hard</option></select></div><div class="field"><label for="questionCount">Questions</label><select id="questionCount" name="questionCount">${[3,5,8,10,12,15].map(n=>`<option value="${n}" ${n===5?'selected':''}>${n} questions</option>`).join('')}</select></div><div class="field full"><div class="field-error" data-error="form"></div><button class="button primary" type="submit">Begin practice <span>→</span></button></div></form></section><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Your sessions</h2><a href="/interview-history" data-route="/interview-history" class="panel-meta">Full history →</a></div><div id="interviews-list"><div class="skeleton"></div></div></section></div></div>`;}
function interviewsMarkup(items){if(!items.length)return empty('Your first session is waiting','Start an interview when you’re ready. Sessions in progress will be saved automatically.');return `<div class="grid">${items.slice(0,5).map(i=>`<article class="role-match"><div><strong style="font-size:12px">${esc(i.role)}</strong><div class="panel-meta" style="margin-top:5px">${esc(i.difficulty)} · ${i.answeredCount||0}/${i.questionCount} answered</div></div><div style="text-align:right"><span class="badge ${i.status==='COMPLETED'?'green':'amber'}">${i.status==='COMPLETED'?'Complete':'In progress'}</span><div style="margin-top:8px"><button class="button secondary small" data-route="/interviews/${i.id}">${i.status==='COMPLETED'?'Review':'Continue'} →</button></div></div></article>`).join('')}</div>`;}
function interviewDetailPage(id){return `<div>${pageHead('MOCK INTERVIEW','One question at a time','Take a moment to think. Your answer doesn’t need to be perfect to be useful.')}<section class="panel panel-pad" id="interview-content"><div class="skeleton"></div></section></div>`;}
function interviewMetricMarkup(question){
 const metrics=[['Technical accuracy',question.technicalCorrectness],['Completeness',question.completeness],['Clarity',question.clarity],['Relevance',question.relevance],['Communication',question.communication]];
 return `<div class="grid two-col mt-16">${metrics.map(([label,value])=>`<div class="role-match"><span class="panel-meta">${label}</span><strong>${interviewScore(value)}</strong></div>`).join('')}</div>`;
}
function interviewFeedbackMarkup(question){
 return `<section class="panel panel-pad mt-16"><div class="panel-head"><h3 class="panel-title">Feedback on your previous answer</h3><span class="badge">${interviewScore(question.score)}</span></div><p class="panel-meta">${esc(question.category)} · Question ${question.questionNumber}</p><p style="font-size:12px;line-height:1.6">${esc(question.feedback||'Feedback is not available for this response.')}</p>${interviewMetricMarkup(question)}</section>`;
}
function interviewDetailMarkup(interview){
 const qs=safeArray(interview.questions), idx=Math.min(Number(interview.currentQuestionIndex)||0,Math.max(qs.length-1,0)), q=qs[idx];
 if(interview.status==='COMPLETED')return `<div class="panel-head"><div><span class="badge green">Session complete</span><h2 class="panel-title" style="margin-top:12px">${esc(interview.role)}</h2></div><strong class="match-number">${interviewScore(interview.overallScore)}</strong></div><p class="subhead">Your overall practice score reflects this session—not a hiring prediction.</p><div class="grid mt-20">${qs.map(question=>`<article class="panel panel-pad"><div class="panel-head"><span class="question-meta">Question ${question.questionNumber} · ${esc(question.category)}</span><span class="badge">${interviewScore(question.score)}</span></div><p style="font-size:13px;line-height:1.6">${esc(question.prompt)}</p><p class="panel-meta">Your answer</p><p style="font-size:11px;line-height:1.65;color:#b2c5d3">${esc(question.answer||'—')}</p><div class="notice-demo">${esc(question.feedback||'Feedback is not available for this response.')}</div>${interviewMetricMarkup(question)}</article>`).join('')}</div>`;
 if(!q)return apiError(new Error('No questions were returned for this session.'),'retry-interview');
 const previous=qs[idx-1];
 return `${previous?.answered?interviewFeedbackMarkup(previous):''}<div class="panel-head"><div><span class="question-meta">${esc(q.category)} · QUESTION ${q.questionNumber} OF ${qs.length}</span><h2 class="panel-title" style="margin-top:9px">${esc(interview.role)}</h2></div><div class="progress-dots" aria-label="Question ${idx+1} of ${qs.length}">${qs.map((_,i)=>`<i class="${i<=idx?'active':''}"></i>`).join('')}</div></div><p class="interview-question">${esc(q.prompt)}</p>${interview.assessmentMode==='DEMO'?'<div class="notice-demo">DEMO session · practice feedback is illustrative.</div>':''}<form class="mt-20" data-form="answer" data-question="${q.id}"><div class="field"><label for="answer">Your answer</label><textarea class="answer-area" id="answer" name="answer" required maxlength="6000" placeholder="Talk through your thinking. You can include context, trade-offs, and what you learned…">${esc(q.answer||'')}</textarea><span class="field-help">Your answer is saved when you submit.</span></div><div class="field-error mt-16" data-error="form"></div><div class="form-actions"><button class="button primary" type="submit">${idx===qs.length-1?'Finish session':'Submit answer & continue'} <span>→</span></button></div></form>`;
}
function historyPage(){return `<div>${pageHead('REFLECT & GROW','Interview history','Notice what’s getting stronger and where you want to focus next.')}<section id="history-data" class="panel panel-pad"><div class="skeleton"></div></section><div id="analytics-data" class="grid content-grid mt-16"></div></div>`;}
function historyMarkup(items){if(!items.length)return empty('Your history begins with your first practice','Start a mock interview to build a record of your progress.',button('Start an interview','go-interviews','secondary','small'));return `<div class="panel-head"><h2 class="panel-title">Past sessions</h2><span class="panel-meta">${items.length} total</span></div><div class="table-wrap"><table><thead><tr><th>Role</th><th>Date</th><th>Level</th><th>Questions</th><th>Score</th><th>Status</th><th></th></tr></thead><tbody>${items.map(i=>`<tr><td>${esc(i.role)}</td><td>${dateLabel(i.createdAt)}</td><td>${esc(i.difficulty)}</td><td>${i.answeredCount}/${i.questionCount}</td><td>${interviewScore(i.overallScore)}</td><td><span class="badge ${i.status==='COMPLETED'?'green':'amber'}">${i.status==='COMPLETED'?'Completed':'In progress'}</span></td><td><button class="button secondary small" data-route="/interviews/${i.id}">${i.status==='COMPLETED'?'Review':'Continue'} →</button></td></tr>`).join('')}</tbody></table></div>`;}
function analyticsMarkup(a){
 const points=safeArray(a.scoreHistory);
  return `<section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Score trend</h2><span class="panel-meta">Completed sessions · scale 0–10</span></div>${points.length?`<div class="bar-chart">${points.map(p=>`<div class="bar-item"><i style="height:${Math.max(7,Math.min(100,(Number(p.score)||0)*10))}%"></i><span>${dateLabel(p.date).replace(/,\s*\d{4}/,'')}</span></div>`).join('')}</div>`:empty('No trend yet','Finish a few sessions to see your learning curve.')}</section><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Reflection points</h2><span class="panel-meta">From your practice</span></div><div class="role-match"><div><div class="panel-meta">Strongest topic</div><strong>${esc(a.strongestTopic||'—')}</strong></div><span class="badge green">Strength</span></div><div class="role-match"><div><div class="panel-meta">A topic to revisit</div><strong>${esc(a.weakestTopic||'—')}</strong></div><span class="badge amber">Focus</span></div><div class="role-match"><div><div class="panel-meta">Improvement trend</div><strong>${a.improvementTrend>0?'+':''}${Number(a.improvementTrend??0).toFixed(1)} points / 10</strong></div><span class="match-number">${interviewScore(a.averageScore)}</span></div></section>`;
}
function matchingPage(){return `<div>${pageHead('FIND YOUR NEXT DIRECTION','Role matching','Compare what you know with role requirements, then pick a useful skill to build.')}<section class="panel panel-pad mb-16"><div class="panel-head"><h2 class="panel-title">Choose a role to compare</h2><span class="panel-meta">Uses only the skills you provide</span></div><form class="form-grid" data-form="match"><div class="field"><label for="roleSelect">Role</label><select id="roleSelect" name="role" required><option value="">Loading roles…</option></select></div><div class="field"><label for="skillsInput">Your skills</label><input id="skillsInput" name="skills" value="${esc(safeArray(appState.user?.skills).join(', '))}" placeholder="JavaScript, SQL, Git" required></div><div class="field full"><div class="field-error" data-error="form"></div><button class="button primary" type="submit">Compare skills →</button></div></form></section><div id="match-result"></div></div>`;}
function matchMarkup(match){
 const percent=Math.min(100,Math.max(0,Number(match.matchPercentage)||0));
 return `<section class="panel panel-pad"><div class="panel-head"><div><span class="eyebrow">YOUR CURRENT ALIGNMENT</span><h2 class="panel-title">${esc(match.role)}</h2></div><div class="score-ring" style="--score:${percent}%"><strong>${percent}%</strong></div></div><p class="subhead">A skills comparison, not a hiring prediction.</p><div class="grid two-col mt-16"><section class="panel panel-pad"><div class="panel-head"><h3 class="panel-title">Skills you already bring</h3></div><div class="tag-list">${safeArray(match.matchingSkills).map(s=>`<span class="badge green">${esc(s)}</span>`).join('')||'<span class="panel-meta">No overlaps yet.</span>'}</div></section><section class="panel panel-pad"><div class="panel-head"><h3 class="panel-title">Skills to explore</h3></div><div class="tag-list">${safeArray(match.missingSkills).map(s=>`<span class="tag">${esc(s)}</span>`).join('')||'<span class="panel-meta">No gaps returned.</span>'}</div></section></div><section class="panel panel-pad mt-16"><div class="panel-head"><h3 class="panel-title">A learning order to consider</h3><span class="panel-meta">Start with the first one</span></div>${safeArray(match.learningOrder).length?`<ol class="list-clean">${match.learningOrder.map(x=>`<li>${esc(x)}</li>`).join('')}</ol>`:'<p class="panel-meta">No learning sequence available.</p>'}</section></section>`;
}
function planPage(){return `<div>${pageHead('STEADY PROGRESS','Your eight-week roadmap','A flexible sequence of topics for the role you’re working toward. Small steps, real momentum.')}<div id="plan-content"><div class="skeleton"></div></div></div>`;}
function planMarkup(plan){
 const topics=safeArray(plan.topics); const weeks=new Map();
 topics.forEach(t=>{const list=weeks.get(t.week)||[];list.push(t);weeks.set(t.week,list);});
 if(!topics.length)return empty('Your roadmap is ready to shape','Once a study plan is available for your profile, it will appear here.');
 return `<section class="panel panel-pad mb-16"><div class="panel-head"><div><h2 class="panel-title">${esc(plan.targetRole||appState.user?.targetRole||'Career')} learning plan</h2><span class="panel-meta">Eight weeks · flexible pace</span></div><strong class="match-number">${plan.completionPercentage??0}%</strong></div><div class="progress-line"><i style="width:${Math.min(100,Number(plan.completionPercentage)||0)}%"></i></div></section><div class="roadmap">${[...weeks.entries()].sort((a,b)=>a[0]-b[0]).map(([week,items])=>`<div class="panel panel-pad"><div class="panel-head"><div class="week-no">WEEK ${week}</div><span class="panel-meta">${items.filter(t=>t.completed).length} / ${items.length} complete</span></div>${items.map(t=>`<article class="week-card ${t.completed?'done':''}"><div class="week-no">${t.completed?'✓':'TOPIC'}</div><div><h3>${esc(t.title)}</h3><p>${esc(t.description)}</p></div><button class="button ${t.completed?'secondary':'ghost'} small" data-toggle-topic="${t.id}" data-completed="${!t.completed}">${t.completed?'Mark active':'Mark complete'}</button></article>`).join('')}</div>`).join('')}</div>`;
}
function settingsPage(){return `<div>${pageHead('ACCOUNT','Settings','Keep your account access in good shape.')}<div class="grid content-grid"><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Account details</h2></div><p style="font-size:12px">${esc(appState.user?.fullName||'')}</p><p class="panel-meta">${esc(appState.user?.email||'')}</p><div class="mt-20">${button('Edit professional profile','go-profile','secondary')}</div></section><section class="panel panel-pad"><div class="panel-head"><h2 class="panel-title">Change password</h2></div><p class="subhead">Use a unique password with at least 10 characters.</p><form class="form-grid" data-form="password"><div class="field full"><label for="current-password">Current password</label><input id="current-password" type="password" name="currentPassword" autocomplete="current-password" required></div><div class="field full"><label for="new-password">New password</label><input id="new-password" type="password" name="newPassword" autocomplete="new-password" minlength="10" required></div><div class="field full"><button class="button primary" type="submit">Update password</button></div><div class="field-error full" data-error="form"></div><p class="panel-meta full">Your current session remains active. Existing sessions expire with their sign-in token.</p></form></section></div><section class="panel panel-pad mt-16"><div class="panel-head"><div><h2 class="panel-title">Sign out</h2><p class="subhead">End this session on this device.</p></div>${button('Sign out','logout','danger')}</div></section></div>`;}
function notFound(){return `<div class="panel panel-pad">${pageHead('NOT FOUND','This page isn’t here','The link may have moved. Your workspace is still right where you left it.',button('Back to overview','go-dashboard','primary'))}</div>`;}
function currentRoute(){const p=location.pathname;if(p==='/')return '/dashboard';return p.replace(/\/$/,'')||'/dashboard';}
function pageFor(path){
 if(path==='/login'||path==='/register')return authView(path==='/register');
 if(path==='/dashboard')return layout(dashboardPage(),'dashboard');
 if(path==='/profile')return layout(profilePage(),'profile');
 if(path==='/resume')return layout(resumesPage(),'resumes');
 if(path==='/resume-analysis')return layout(resumeAnalysisPage(),'analysis');
 if(path==='/interviews')return layout(interviewsPage(),'interviews');
 if(path==='/interview-history')return layout(historyPage(),'history');
 if(path==='/job-matching')return layout(matchingPage(),'matching');
 if(path==='/study-plan')return layout(planPage(),'plan');
 if(path==='/settings')return layout(settingsPage(),'settings');
 const detail=path.match(/^\/interviews\/(\d+)$/);
 if(detail)return layout(interviewDetailPage(detail[1]),'interviews');
 return layout(notFound(),'dashboard');
}
async function mountData(path){
 const page=path;
 if(!appState.user){
  try{appState.user=await request('/users/me');syncUser();}
  catch(error){if(error.status===401)navigate('/login');}
  if(!appState.user)return;
 }
 if(page==='/dashboard'){
  const target=document.querySelector('.dashboard-loading');
  try{const data=await request('/dashboard');if(target?.isConnected)target.outerHTML=dashboardMarkup(data);}
  catch(e){if(target?.isConnected)target.outerHTML=apiError(e,'retry-dashboard');}
 }
 if(page==='/profile'){
  const target=document.querySelector('#profile-content');
  try{const user=await request('/users/me');appState.user=user;if(target?.isConnected)target.outerHTML=profileMarkup(user);syncUser();}
  catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-profile');}
 }
 if(page==='/resume'){
  const target=document.querySelector('#resumes-list');
  try{const list=await request('/resumes');if(target?.isConnected)target.innerHTML=resumeListMarkup(safeArray(list));}
  catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-resumes');}
 }
 if(page==='/resume-analysis'){
  const id=new URLSearchParams(location.search).get('id');
  if(id){const target=document.querySelector('#analysis-content');try{const data=await request(`/resumes/${encodeURIComponent(id)}/analysis`);if(target?.isConnected)target.innerHTML=analysisMarkup(data);}catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-analysis');}}
  else{const target=document.querySelector('#analysis-picker');try{const items=await request('/resumes');if(target?.isConnected)target.innerHTML=items.length?`<div class="grid">${items.map(r=>`<button class="quick-action" data-route="/resume-analysis?id=${r.id}"><span class="qa-icon">▤</span><strong>${esc(r.fileName)}</strong><span>${esc(r.targetRole)} · ${dateLabel(r.createdAt)}</span></button>`).join('')}</div>`:empty('No resume to review','Upload a resume first to see skills, strengths, and suggestions.',button('Go to resume library','go-resumes','secondary','small'));}catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-analysis');}}
 }
 if(page==='/interviews'){
  const target=document.querySelector('#interviews-list');try{const data=await request('/interviews');if(target?.isConnected)target.innerHTML=interviewsMarkup(safeArray(data));}catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-interviews');}
 }
 if(page==='/interview-history'){
  const target=document.querySelector('#history-data');try{const data=await request('/interviews');if(target?.isConnected)target.innerHTML=historyMarkup(safeArray(data));}catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-history');}
  const analytics=document.querySelector('#analytics-data');try{const data=await request('/analytics');if(analytics?.isConnected)analytics.innerHTML=analyticsMarkup(data);}catch(e){if(analytics?.isConnected)analytics.innerHTML=apiError(e,'retry-analytics');}
 }
 if(page==='/job-matching'){
  const select=document.querySelector('#roleSelect');try{const roles=await request('/job-roles');if(select?.isConnected)select.innerHTML=`<option value="">Select a role</option>${safeArray(roles).map(r=>`<option value="${esc(r.name)}">${esc(r.name)}</option>`).join('')}`;}catch(e){if(select?.isConnected)select.innerHTML='<option value="">Roles could not be loaded</option>';const form=document.querySelector('[data-form="match"]');if(form?.isConnected)form.querySelector('[data-error="form"]').textContent=e.message;}
 }
 if(page==='/study-plan'){
  const target=document.querySelector('#plan-content');try{const data=await request('/study-plan');if(target?.isConnected)target.innerHTML=planMarkup(data);}catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-plan');}
 }
 const match=page.match(/^\/interviews\/(\d+)$/);
 if(match){const target=document.querySelector('#interview-content');try{const data=await request(`/interviews/${match[1]}`);if(target?.isConnected)target.innerHTML=interviewDetailMarkup(data);}catch(e){if(target?.isConnected)target.innerHTML=apiError(e,'retry-interview');}}
}
function syncUser(){const avatar=document.querySelector('.user-mini .avatar'),strong=document.querySelector('.user-mini strong'),span=document.querySelector('.user-mini span');if(avatar)avatar.textContent=initials(appState.user?.fullName);if(strong)strong.textContent=appState.user?.fullName||'Your workspace';if(span)span.textContent=appState.user?.targetRole||appState.user?.email||'Set your target role';}
function render(){
 const path=currentRoute();
 document.title=`${path==='/dashboard'?'Overview':path.replace(/^\//,'').replaceAll('-',' ').replace(/\b\w/g,s=>s.toUpperCase())} · CareerSphere`;
 root.innerHTML=pageFor(path);
 if(path!=='/login'&&path!=='/register')mountData(path);
}
function formValues(form){return Object.fromEntries(new FormData(form).entries());}
function setError(form,message,fieldErrors={}){
 const general=form.querySelector('[data-error="form"]');if(general)general.textContent=message||'';
 for(const [field,msg] of Object.entries(fieldErrors)){const el=form.querySelector(`[data-error="${CSS.escape(field)}"]`);if(el)el.textContent=msg;}
}
function setBusy(form,busy,label){
 const button=form.querySelector('button[type="submit"]');if(!button)return;
 button.disabled=busy;if(busy){button.dataset.original=button.innerHTML;button.innerHTML='Working…';}else if(button.dataset.original){button.innerHTML=button.dataset.original;delete button.dataset.original;}
}
async function submitForm(form){
 const kind=form.dataset.form, values=formValues(form);setError(form,'');setBusy(form,true);
 try{
  if(kind==='login'||kind==='register'){
   const body={...values};
   if(kind==='register'){for(const field of ['phone','location','education'])if(!body[field])delete body[field];body.graduationYear=body.graduationYear?Number(body.graduationYear):undefined;}
   const data=await request(`/auth/${kind}`,{method:'POST',body:JSON.stringify(body)});appState.user=data.user;toast(kind==='login'?'Welcome back.':'Your workspace is ready.');navigate('/dashboard');return;
  }
  if(kind==='password'){
   await request('/auth/change-password',{method:'POST',body:JSON.stringify({currentPassword:values.currentPassword,newPassword:values.newPassword})});
   form.reset();toast('Your password has been changed.');return;
  }
  if(kind==='profile'){
   const body={...values,graduationYear:values.graduationYear?Number(values.graduationYear):null,skills:values.skills.split(',').map(s=>s.trim()).filter(Boolean)};
   delete body.email;for(const key of ['phone','location','education','targetRole','githubUrl','linkedinUrl'])if(body[key]==='')body[key]=null;
   appState.user=await request('/users/me',{method:'PUT',body:JSON.stringify(body)});toast('Profile saved.');render();return;
  }
  if(kind==='upload'){
   const file=form.querySelector('[name=file]').files[0];if(!file){setError(form,'Choose a PDF file to continue.');return;}
   if(file.type!=='application/pdf'&&!file.name.toLowerCase().endsWith('.pdf')){setError(form,'Please choose a PDF document.');return;}
   const data=new FormData();data.append('file',file);data.append('targetRole',values.targetRole);
   const result=await request('/resumes/upload',{method:'POST',body:data});toast('Resume uploaded. Your insights are ready.');navigate(`/resume-analysis?id=${result.id}`);return;
  }
  if(kind==='improve'){
   const result=await request('/resume-improvements',{method:'POST',body:JSON.stringify({...values})});
   const target=form.querySelector('#improvement-result');target.innerHTML=`<div class="panel panel-pad"><div class="panel-head"><h3 class="panel-title">A suggestion to work from</h3><span class="badge ${result.assessmentMode==='DEMO'?'amber':'green'}">${esc(result.assessmentMode)}</span></div><p class="panel-meta">Original</p><p style="font-size:12px;line-height:1.6">${esc(result.original)}</p><p class="panel-meta">Suggested revision</p><p style="font-size:12px;line-height:1.65;color:#d4e6f1">${esc(result.suggestion)}</p>${result.assessmentMode==='DEMO'?'<div class="notice-demo">DEMO suggestion · review carefully and keep your own voice.</div>':''}</div>`;return;
  }
  if(kind==='create-interview'){
   const data=await request('/interviews',{method:'POST',body:JSON.stringify({...values,questionCount:Number(values.questionCount)})});toast('Practice session created.');navigate(`/interviews/${data.id}`);return;
  }
  if(kind==='answer'){
   const id=currentRoute().match(/^\/interviews\/(\d+)$/)?.[1];if(!id)throw new Error('Interview session not found.');
   const result=await request(`/interviews/${id}/answer`,{method:'POST',body:JSON.stringify({questionId:Number(form.dataset.question),answer:values.answer})});
   if(result.completed)toast('Session complete. Take a moment to review your feedback.');render();return;
  }
  if(kind==='match'){
   const result=await request('/job-matches',{method:'POST',body:JSON.stringify({role:values.role,skills:values.skills.split(',').map(s=>s.trim()).filter(Boolean)})});
   document.querySelector('#match-result').innerHTML=matchMarkup(result);return;
  }
 }catch(error){
  setError(form,error.message,error.fieldErrors||{});
  if(error.status===401&&(kind==='profile'||kind==='upload'||kind==='create-interview')){setError(form,'Your session may have expired. Please sign in again.');}
 }finally{if(form.isConnected)setBusy(form,false);}
}
document.addEventListener('click',async event=>{
 const link=event.target.closest('[data-route]');if(link){event.preventDefault();navigate(link.dataset.route);return;}
 const action=event.target.closest('[data-action]')?.dataset.action;
 if(action==='toggle-menu'){appState.drawer=!appState.drawer;document.querySelector('.sidebar')?.classList.toggle('open',appState.drawer);document.querySelector('.mobile-overlay')?.classList.toggle('show',appState.drawer);return;}
 if(action==='close-menu'){appState.drawer=false;document.querySelector('.sidebar')?.classList.remove('open');document.querySelector('.mobile-overlay')?.classList.remove('show');return;}
 if(action==='go-profile')navigate('/profile');if(action==='go-interviews')navigate('/interviews');if(action==='go-dashboard')navigate('/dashboard');if(action==='go-resumes')navigate('/resume');
 if(action==='focus-upload'){document.querySelector('#resume-file')?.focus();document.querySelector('#resume-file')?.click();}
 if(action==='logout'){try{await request('/auth/logout',{method:'POST'});}catch{}appState.user=null;toast('You’ve signed out.');navigate('/login');}
 const resume=event.target.closest('[data-resume]');if(resume)navigate(`/resume-analysis?id=${resume.dataset.resume}`);
 const toggle=event.target.closest('[data-toggle-topic]');if(toggle){
  toggle.disabled=true;try{await request(`/study-plan/topics/${toggle.dataset.toggleTopic}`,{method:'PUT',body:JSON.stringify({completed:toggle.dataset.completed==='true'})});toast('Roadmap updated.');render();}catch(e){toggle.disabled=false;toast(e.message);}
 }
 if(action?.startsWith('retry-')){const key=action.replace('retry-','');const routes={dashboard:'/dashboard',profile:'/profile',resumes:'/resume',analysis:'/resume-analysis',interviews:'/interviews',history:'/interview-history',analytics:'/interview-history',plan:'/study-plan',interview:currentRoute()};if(routes[key]){if(key==='analysis'&&new URLSearchParams(location.search).has('id')){render();return;}render();}return;}
});
document.addEventListener('submit',event=>{const form=event.target.closest('form[data-form]');if(form){event.preventDefault();submitForm(form);}});
document.addEventListener('change',event=>{if(event.target.matches('#resume-file')){const label=document.querySelector('[data-file-label]');if(label)label.textContent=event.target.files[0]?.name||'Maximum file size depends on your workspace';}});
window.addEventListener('popstate',render);
render();
