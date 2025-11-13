const img = document.getElementById("frame") as HTMLImageElement;
const fpsSpan = document.getElementById("fps") as HTMLElement;
const resSpan = document.getElementById("res") as HTMLElement;

let last = performance.now();
let frames = 0;

function updateClock(){
  const now = performance.now();
  frames++;
  if (now - last > 1000) {
    fpsSpan.innerText = (frames / ((now - last)/1000)).toFixed(1);
    frames = 0;
    last = now;
  }
  requestAnimationFrame(updateClock);
}

img.src = "sample_output.png";
img.onload = () => {
  resSpan.innerText = `${img.naturalWidth}x${img.naturalHeight}`;
};

updateClock();
