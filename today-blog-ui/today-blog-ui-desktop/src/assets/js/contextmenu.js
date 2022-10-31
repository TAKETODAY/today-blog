/*
===============================================================

Hi! Welcome to my little playground!

My name is Tobias Bogliolo. 'Open source' by default and always 'responsive',
I'm a publicist, visual designer and frontend developer based in Barcelona. 

Here you will find some of my personal experiments. Sometimes usefull,
sometimes simply for fun. You are free to use them for whatever you want 
but I would appreciate an attribution from my work. I hope you enjoy it.

===============================================================
*/

$(document).ready(function(){
  //Show contextmenu:
  $(document).contextmenu(function(e){
    //Get window size:
    var winWidth = $(document).width();
    var winHeight = $(document).height();
    //Get pointer position:
    var posX = e.pageX;
    var posY = e.pageY;
    //Get contextmenu size:
    var menuWidth = $(".contextmenu").width();
    var menuHeight = $(".contextmenu").height();
    //Security margin:
    var secMargin = 10;
    //Prevent page overflow:
    if(posX + menuWidth + secMargin >= winWidth
    && posY + menuHeight + secMargin >= winHeight){
      //Case 1: right-bottom overflow:
      posLeft = posX - menuWidth - secMargin + "px";
      posTop = posY - menuHeight - secMargin + "px";
    }
    else if(posX + menuWidth + secMargin >= winWidth){
      //Case 2: right overflow:
      posLeft = posX - menuWidth - secMargin + "px";
      posTop = posY + secMargin + "px";
    }
    else if(posY + menuHeight + secMargin >= winHeight){
      //Case 3: bottom overflow:
      posLeft = posX + secMargin + "px";
      posTop = posY - menuHeight - secMargin + "px";
    }
    else {
      //Case 4: default values:
      posLeft = posX + secMargin + "px";
      posTop = posY + secMargin + "px";
    };
    //Display contextmenu:
    $(".contextmenu").css({
      "left": posLeft,
      "top": posTop
    }).show();
    //Prevent browser default contextmenu.
    return false;
  });
  //Hide contextmenu:
  $(document).click(function(){
    $(".contextmenu").hide();
  });
});