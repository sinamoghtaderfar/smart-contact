$("#menu-toggle").click(function(e) {
  e.preventDefault();
  $("#wrapper").toggleClass("toggled");
});


// search show contacts.html
// document.querySelector('.search-result').style.display = 'none';
// $(".search-result").css("display", "none")
const search = () =>{
  // console.log("searching");

  let query = $(".searching").val()

  if(query == ''){
    $(".search-result").hide();
  }else {
    console.log(query);
    // // send request to server
    let url = `http://localhost:8282/search/${query}`;

    fetch(url).then((response)=>{

      return response.json();

    }).then((data)=>{
      console.log(data);
      let text = `<div class="list-group">`;
          data.forEach((contact)=>{
            text +=`<a href="/user/${contact.cId}/contact" class="list-group-item list-group-action"> ${contact.name}</a>`
          })
            text += `</div>`;
          $(".search-result").html(text);
          $(".search-result").show();
    });
  }
}

//first request to server to create order
const paymentStart= () =>{

  let amount = $("#payment_field").val();
  console.log(amount)
  if(amount == '' || amount == null){
    swal("Failed!", "amount is required !!", "error");

    return;
  }

  // use ajax to send request to server to create order...
  $.ajax({
    url:'/user/create_order',
    data:JSON.stringify({amount:amount,info:'order_request'}),
    contentType:'application/json',
    type:'POST',
    dataType:'json',
    success:function (response){
      // success
      console.log(response);
      if(response.status == "created"){
        //forward payment form

        let options = {
          key:'rzp_test_Ypve9lgozIqArN',
          amount:response.amount,
          currency:'INR',
          name:'Smart Contact Manager',
          description:'Donation',
          image:'http://localhost:8282/img/contact-logo.png',
          order_id:response.id,
          handler:function (response){
            console.log(response.razorpay_payment_id)
            console.log(response.razorpay_order_id)
            console.log(response.razorpay_signature)
            console.log('payment successfully')
            updatePaymentOnServer(response.razorpay_payment_id,response.razorpay_order_id,'paid');
          },
          prefill:{
            name: "",
            email:"",
            contact:""
          },
          notes:{
            address:"sina moghtaderfar",
          },
          theme:{
            color:"#3399cc",
          }
        };

        let rzp = new Razorpay(options);

        rzp.on('payment.failed',function (response) {
          console.log(response.error.code);
          console.log(response.error.description);
          console.log(response.error.source);
          console.log(response.error.step);
          console.log(response.error.reason);
          console.log(response.error.metadata.order_id);
          console.log(response.error.metadata.payment_id);
          swal("Failed!", "Payment failed!!", "error");

        });

        rzp.open();

      }
    },
    error:function (error){
      //error
      console.log(error)
      alert("something went wrong!!")
    },
  });

};

function updatePaymentOnServer(payment_id,order_id,status){
  $.ajax({
    url: '/user/update_order',
    data: JSON.stringify({payment_id: payment_id, order_id: order_id,status:status}),
    contentType: 'application/json',
    type: 'POST',
    dataType: 'json',
    success:function (response) {
      swal("Good job!", "Payment successful !!", "success");
    },
    error:function (){
      swal("Failed!", "Your Payment is successful , but we did not get on server , we will contact you as soon as possible!!", "error");

    },
  })
}