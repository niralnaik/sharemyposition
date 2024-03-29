//
//  sharemyposition_iphoneViewController.m
//  sharemyposition-iphone
//
//  Created by Sylvain Maucourt on 28/06/10.
//  Copyright Deveryware 2010. All rights reserved.
//

#import "sharemyposition_iphoneViewController.h"

@implementation sharemyposition_iphoneViewController


@synthesize lastLocation;

/*
// The designated initializer. Override to perform setup that is required before the view is loaded.
- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    if ((self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil])) {
        // Custom initialization
    }
    return self;
}
*/

/*
// Implement loadView to create a view hierarchy programmatically, without using a nib.
- (void)loadView {
}
*/


// Implement viewDidLoad to do additional setup after loading the view, typically from a nib.
- (void)viewDidLoad {
	locationController = [[MyCLController alloc] init];
	locationController.delegate = self;
    [super viewDidLoad];
	[self locateMeNow:self];
}


/*
// Override to allow orientations other than the default portrait orientation.
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
    // Return YES for supported orientations
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}
*/

- (void)didReceiveMemoryWarning {
	// Releases the view if it doesn't have a superview.
    [super didReceiveMemoryWarning];
	
	// Release any cached data, images, etc that aren't in use.
}

- (void)viewDidUnload {
	// Release any retained subviews of the main view.
	// e.g. self.myOutlet = nil;
}


- (void)dealloc {
    [locationController release];
	[lastLocation release];
    [super dealloc];
}

-(IBAction)locateMeNow:(id)sender {
	NSLog(@"locate me now ...");
	[activity startAnimating];
    [locationController.locationManager startUpdatingLocation];
}

- (void)locationUpdate:(CLLocation*)location {
	NSLog(@"update location with %@", [location description]);
	
	[activity stopAnimating];
	
	NSTimeInterval locationAge = -[location.timestamp timeIntervalSinceNow];
    if (locationAge > 5.0) {
		NSLog(@"you will not want to rely on cached measurements");
		return;
	}
	
	if (lastLocation == nil || lastLocation.horizontalAccuracy > location.horizontalAccuracy) {
		self.lastLocation = location;
	} else {
		NSLog(@"nothing to do because lastLocation is more accurate.");
		return;
	}
	
	NSURL *url = [NSURL URLWithString:
				   [NSString stringWithFormat:@"http://sharemyposition.appspot.com/sharedmap.jsp?pos=%f,%f&size=320x220",
						location.coordinate.latitude,
						location.coordinate.longitude
				   ]
				  ];
	NSLog(@"loading url .. %@", url);
	
	[preview loadRequest:[NSURLRequest requestWithURL:url]];
	geocodeAddressSwitch.enabled=YES;
	shareBySMS.enabled=YES;
}

- (void)locationError:(NSError*)error {
	NSLog(@"error %@", [error description]);
	[activity stopAnimating];
}

- (IBAction)shareItBySMS:(id)sender {
	NSLog(@"share by SMS selected");
	NSString *shareIt = [self shareIt];
	NSLog(@"result:%@", shareIt);
}

- (NSString*)shareIt {
	NSLog(@"treating lastLocation ... %@", [self.lastLocation description]);
	
	[self stopRequestLocation];
	
	NSString *url = [NSString 
					 stringWithFormat:@"http://sharemyposition.appspot.com/static.jsp?pos=%f,%f",
					 self.lastLocation.coordinate.latitude, 
					 self.lastLocation.coordinate.longitude
					];
	
	NSString *shortenedUrl = [self shorteningUrl:url];
	NSString *address;
	
	if ([geocodeAddressSwitch isOn]) {
		address = [self gettingAddress];
	} else {
		address = @"";
	}
	
	return [NSString stringWithFormat:@"I am currently here, %@ %@",
			address, shortenedUrl];
}

- (void)stopRequestLocation {
	NSLog(@"stopping request location");
    [locationController.locationManager stopUpdatingLocation];
}

- (NSString*)gettingAddress {
	//TODO: ici la detection de l'adresse
	return @"toto";
}

- (NSString*)shorteningUrl:(NSString*)url {

	NSURLRequest *theRequest=[NSURLRequest requestWithURL:
							  [NSURL
								URLWithString:
									[NSString 
									 stringWithFormat:@"http://sharemyposition.appspot.com/service/create?url=%@",
									 url
									]
							  ]
							 ];
	
	NSURLResponse *response;
	NSError *error;
	NSData *data = [NSURLConnection 
						sendSynchronousRequest:theRequest
						returningResponse:&response 
						error:&error
					];
	
	NSLog(@"request:%@", theRequest);
	//TODO: gestion des erreurs
	return [[NSString alloc] initWithBytes:[data bytes]
							  length:[data length] encoding: NSUTF8StringEncoding];
}

@end
