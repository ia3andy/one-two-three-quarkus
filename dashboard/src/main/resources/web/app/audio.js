import {Howl, Howler} from 'howler';

const tracks = [
    { src: "/static/audio/best-time.mp3", title: "Best Time - FASSounds" },
    { src: "/static/audio/fun-punk.mp3", title: "Fun Punk Opener - LiteSaturation" },
    { src: "/static/audio/hitting-hard.mp3", title: "Hitting Hard - Alex Kizenkov" },
    { src: "/static/audio/stomping-rock.mp3", title: "Four Shots - Alex Grohl" },
];

const track = tracks[Math.floor(Math.random() * tracks.length)];

window.audio = {
    track,
    game: new Howl({src: [track.src], loop: true, html5: true}),
};


