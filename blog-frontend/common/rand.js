const allLabels = ["default", "primary", "success", "info", "warning", "danger"]
function getRandLabel() {
  return `label label-${randLabel()}`
}

function randLabel() {
  return allLabels[rand(0, 5)]
}

function rand(n, m) {
  return Math.floor(Math.random() * (m - n + 1) + n)
}

export {
  rand,
  allLabels,
  randLabel,
  getRandLabel,
};
